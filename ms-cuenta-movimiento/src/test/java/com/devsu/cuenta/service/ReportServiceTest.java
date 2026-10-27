package com.devsu.cuenta.service;

import com.devsu.cuenta.client.ClientDTO;
import com.devsu.cuenta.client.ClientService;
import com.devsu.cuenta.dto.ReportDTO;
import com.devsu.cuenta.model.Account;
import com.devsu.cuenta.model.Movement;
import com.devsu.cuenta.repository.AccountRepository;
import com.devsu.cuenta.repository.MovementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final LocalDate FROM = LocalDate.of(2024, 2, 1);
    private static final LocalDate TO = LocalDate.of(2024, 2, 10);
    private static final LocalDateTime FROM_START = FROM.atStartOfDay();
    private static final LocalDateTime TO_END = TO.atTime(LocalTime.MAX);

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private MovementRepository movementRepository;
    @Mock
    private ClientService clientService;
    @InjectMocks
    private ReportService service;

    private static ClientDTO client(String name) {
        ClientDTO dto = new ClientDTO();
        dto.setId(1L);
        dto.setName(name);
        return dto;
    }

    private static Account account(long id, String number, String type, double balance, boolean status) {
        return Account.builder().accountId(id).accountNumber(number).accountType(type)
                .initialBalance(balance).status(status).clientId(1L).build();
    }

    private static Movement movement(long id, LocalDateTime date, double value, double balance, Account account) {
        return Movement.builder().movementId(id).date(date).movementType(value > 0 ? "Deposito" : "Retiro")
                .value(value).balance(balance).account(account).build();
    }

    @Test
    void generaUnaFilaPorMovimientoConLosDatosDeCuentaYCliente() {
        Account acc = account(1L, "478758", "Ahorros", 1425.0, true);
        LocalDateTime d1 = LocalDateTime.of(2024, 2, 5, 10, 30);
        LocalDateTime d2 = LocalDateTime.of(2024, 2, 6, 11, 0);
        when(clientService.findById(1L)).thenReturn(client("Jose Lema"));
        when(accountRepository.findByClientId(1L)).thenReturn(List.of(acc));
        when(movementRepository.findByAccount_AccountIdAndDateBetween(1L, FROM_START, TO_END))
                .thenReturn(List.of(movement(1L, d1, -575.0, 1425.0, acc), movement(2L, d2, 600.0, 2025.0, acc)));

        List<ReportDTO> report = service.getReport(1L, FROM, TO);

        assertThat(report).containsExactly(
                ReportDTO.builder().fecha(d1).cliente("Jose Lema").numeroCuenta("478758").tipo("Ahorros")
                        .saldoInicial(1425.0).estado(true).movimiento(-575.0).saldoDisponible(1425.0).build(),
                ReportDTO.builder().fecha(d2).cliente("Jose Lema").numeroCuenta("478758").tipo("Ahorros")
                        .saldoInicial(1425.0).estado(true).movimiento(600.0).saldoDisponible(2025.0).build());
    }

    @Test
    void convierteElRangoDeFechasDesdeInicioDelDiaHastaElUltimoInstanteDelDiaFinal() {
        Account acc = account(1L, "478758", "Ahorros", 0.0, true);
        when(clientService.findById(1L)).thenReturn(client("Jose Lema"));
        when(accountRepository.findByClientId(1L)).thenReturn(List.of(acc));
        // Si las fechas no coinciden exactamente, el stub no aplica y el mock devuelve lista vacia.
        when(movementRepository.findByAccount_AccountIdAndDateBetween(
                1L, LocalDateTime.of(2024, 2, 1, 0, 0), LocalDateTime.of(2024, 2, 10, 23, 59, 59, 999_999_999)))
                .thenReturn(List.of(movement(1L, LocalDateTime.of(2024, 2, 10, 23, 30), 10.0, 10.0, acc)));

        assertThat(service.getReport(1L, FROM, TO)).hasSize(1);
    }

    @Test
    void incluyeMovimientosDeVariasCuentasDelCliente() {
        Account ahorros = account(1L, "478758", "Ahorros", 1000.0, true);
        Account corriente = account(2L, "225487", "Corriente", 500.0, false);
        LocalDateTime d = LocalDateTime.of(2024, 2, 5, 9, 0);
        when(clientService.findById(1L)).thenReturn(client("Marianela Montalvo"));
        when(accountRepository.findByClientId(1L)).thenReturn(List.of(ahorros, corriente));
        when(movementRepository.findByAccount_AccountIdAndDateBetween(1L, FROM_START, TO_END))
                .thenReturn(List.of(movement(1L, d, 100.0, 1100.0, ahorros)));
        when(movementRepository.findByAccount_AccountIdAndDateBetween(2L, FROM_START, TO_END))
                .thenReturn(List.of(movement(2L, d, -50.0, 450.0, corriente)));

        List<ReportDTO> report = service.getReport(1L, FROM, TO);

        assertThat(report).extracting(ReportDTO::getNumeroCuenta).containsExactly("478758", "225487");
        assertThat(report).extracting(ReportDTO::getTipo).containsExactly("Ahorros", "Corriente");
        assertThat(report).extracting(ReportDTO::getEstado).containsExactly(true, false);
        assertThat(report).extracting(ReportDTO::getCliente).containsOnly("Marianela Montalvo");
    }

    @Test
    void omiteCuentasSinMovimientosEnElRango() {
        Account acc = account(1L, "478758", "Ahorros", 1000.0, true);
        when(clientService.findById(1L)).thenReturn(client("Jose Lema"));
        when(accountRepository.findByClientId(1L)).thenReturn(List.of(acc));
        when(movementRepository.findByAccount_AccountIdAndDateBetween(1L, FROM_START, TO_END)).thenReturn(List.of());

        assertThat(service.getReport(1L, FROM, TO)).isEmpty();
    }

    @Test
    void devuelveListaVaciaSiElClienteNoTieneCuentas() {
        when(clientService.findById(1L)).thenReturn(client("Jose Lema"));
        when(accountRepository.findByClientId(1L)).thenReturn(List.of());

        assertThat(service.getReport(1L, FROM, TO)).isEmpty();
        verify(movementRepository, never()).findByAccount_AccountIdAndDateBetween(anyLong(), any(), any());
    }

    @Test
    void usaNombreGenericoSiElServicioDeClientesFalla() {
        Account acc = account(1L, "478758", "Ahorros", 1000.0, true);
        when(clientService.findById(7L)).thenThrow(new RestClientException("servicio caido"));
        when(accountRepository.findByClientId(7L)).thenReturn(List.of(acc));
        when(movementRepository.findByAccount_AccountIdAndDateBetween(1L, FROM_START, TO_END))
                .thenReturn(List.of(movement(1L, LocalDateTime.of(2024, 2, 5, 9, 0), 10.0, 10.0, acc)));

        List<ReportDTO> report = service.getReport(7L, FROM, TO);

        assertThat(report).hasSize(1);
        assertThat(report.get(0).getCliente()).isEqualTo("Cliente 7");
    }

    @Test
    void usaNombreGenericoSiElServicioDeClientesDevuelveNull() {
        Account acc = account(1L, "478758", "Ahorros", 1000.0, true);
        when(clientService.findById(7L)).thenReturn(null);
        when(accountRepository.findByClientId(7L)).thenReturn(List.of(acc));
        when(movementRepository.findByAccount_AccountIdAndDateBetween(1L, FROM_START, TO_END))
                .thenReturn(List.of(movement(1L, LocalDateTime.of(2024, 2, 5, 9, 0), 10.0, 10.0, acc)));

        assertThat(service.getReport(7L, FROM, TO).get(0).getCliente()).isEqualTo("Cliente 7");
    }
}
