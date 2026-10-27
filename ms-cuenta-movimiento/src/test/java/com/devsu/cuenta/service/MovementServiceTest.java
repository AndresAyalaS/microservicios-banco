package com.devsu.cuenta.service;

import com.devsu.cuenta.dto.MovementDTO;
import com.devsu.cuenta.exception.BusinessRuleException;
import com.devsu.cuenta.exception.InsufficientBalanceException;
import com.devsu.cuenta.exception.ResourceNotFoundException;
import com.devsu.cuenta.model.Account;
import com.devsu.cuenta.model.Movement;
import com.devsu.cuenta.repository.AccountRepository;
import com.devsu.cuenta.repository.MovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovementServiceTest {

    @Mock
    private MovementRepository movementRepository;
    @Mock
    private AccountService accountService;
    @Mock
    private AccountRepository accountRepository;
    @InjectMocks
    private MovementService service;

    @Captor
    private ArgumentCaptor<List<Movement>> savedMovements;

    private static Account account(double balance) {
        return Account.builder()
                .accountId(1L).accountNumber("478758").accountType("Ahorros")
                .initialBalance(balance).status(true).clientId(1L).build();
    }

    private static Movement movement(long id, double value, double balance, Account account) {
        return Movement.builder()
                .movementId(id)
                .date(LocalDateTime.of(2024, 2, (int) id, 10, 0))
                .movementType(value > 0 ? "Deposito" : "Retiro")
                .value(value)
                .balance(balance)
                .account(account)
                .build();
    }

    private static MovementDTO request(Long accountId, Double value) {
        return MovementDTO.builder().accountId(accountId).value(value).build();
    }

    @Nested
    @DisplayName("create")
    class Create {

        private void stubSaveMovement() {
            when(movementRepository.save(any(Movement.class))).thenAnswer(inv -> {
                Movement m = inv.getArgument(0);
                m.setMovementId(100L);
                return m;
            });
        }

        private void stubLastMovement(Optional<Movement> last) {
            when(movementRepository.findTopByAccount_AccountIdOrderByDateDescMovementIdDesc(1L)).thenReturn(last);
        }

        @Test
        void primerDepositoSumaAlSaldoInicial() {
            Account account = account(1000.0);
            when(accountService.findById(1L)).thenReturn(account);
            stubLastMovement(Optional.empty());
            stubSaveMovement();
            LocalDateTime before = LocalDateTime.now();

            MovementDTO result = service.create(request(1L, 600.0));

            assertThat(result.getMovementId()).isEqualTo(100L);
            assertThat(result.getMovementType()).isEqualTo("Deposito");
            assertThat(result.getValue()).isEqualTo(600.0);
            assertThat(result.getBalance()).isEqualTo(1600.0);
            assertThat(result.getAccountId()).isEqualTo(1L);
            assertThat(result.getDate()).isBetween(before, LocalDateTime.now());
            assertThat(account.getInitialBalance()).isEqualTo(1600.0);
            verify(accountRepository).save(account);
        }

        @Test
        void primerRetiroRestaDelSaldoInicial() {
            Account account = account(1000.0);
            when(accountService.findById(1L)).thenReturn(account);
            stubLastMovement(Optional.empty());
            stubSaveMovement();

            MovementDTO result = service.create(request(1L, -575.0));

            assertThat(result.getMovementType()).isEqualTo("Retiro");
            assertThat(result.getBalance()).isEqualTo(425.0);
            assertThat(account.getInitialBalance()).isEqualTo(425.0);
        }

        @Test
        void usaElSaldoDelUltimoMovimientoYNoElDeLaCuenta() {
            Account account = account(999999.0); // valor "viejo" a proposito
            Movement last = movement(5L, -100.0, 300.0, account);
            when(accountService.findById(1L)).thenReturn(account);
            stubLastMovement(Optional.of(last));
            stubSaveMovement();

            MovementDTO result = service.create(request(1L, 50.0));

            assertThat(result.getBalance()).isEqualTo(350.0);
        }

        @Test
        void permiteRetirarTodoElSaldo() {
            when(accountService.findById(1L)).thenReturn(account(1000.0));
            stubLastMovement(Optional.empty());
            stubSaveMovement();

            MovementDTO result = service.create(request(1L, -1000.0));

            assertThat(result.getBalance()).isZero();
        }

        @Test
        void guardaElMovimientoConLosDatosCalculados() {
            Account account = account(1000.0);
            when(accountService.findById(1L)).thenReturn(account);
            stubLastMovement(Optional.empty());
            stubSaveMovement();

            service.create(request(1L, -200.0));

            ArgumentCaptor<Movement> captor = ArgumentCaptor.forClass(Movement.class);
            verify(movementRepository).save(captor.capture());
            Movement saved = captor.getValue();
            assertThat(saved.getValue()).isEqualTo(-200.0);
            assertThat(saved.getMovementType()).isEqualTo("Retiro");
            assertThat(saved.getBalance()).isEqualTo(800.0);
            assertThat(saved.getAccount()).isSameAs(account);
            assertThat(saved.getDate()).isNotNull();
        }

        @Test
        void lanzaSaldoInsuficienteYNoPersisteNada() {
            when(accountService.findById(1L)).thenReturn(account(100.0));
            stubLastMovement(Optional.empty());

            assertThatThrownBy(() -> service.create(request(1L, -150.0)))
                    .isInstanceOf(InsufficientBalanceException.class);

            verify(accountRepository, never()).save(any());
            verify(movementRepository, never()).save(any());
        }

        @Test
        void lanzaSaldoInsuficienteTomandoComoBaseElUltimoMovimiento() {
            Account account = account(5000.0); // la cuenta dice 5000 pero el ultimo saldo es 40
            when(accountService.findById(1L)).thenReturn(account);
            stubLastMovement(Optional.of(movement(2L, -10.0, 40.0, account)));

            assertThatThrownBy(() -> service.create(request(1L, -41.0)))
                    .isInstanceOf(InsufficientBalanceException.class);

            verify(movementRepository, never()).save(any());
        }

        @ParameterizedTest(name = "valor = {0}")
        @NullSource
        @ValueSource(doubles = {0.0})
        void rechazaValorNuloOCero(Double value) {
            assertThatThrownBy(() -> service.create(request(1L, value)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("distinto de cero");

            verifyNoInteractions(movementRepository, accountService, accountRepository);
        }

        @Test
        void rechazaSiNoSeEnviaAccountId() {
            assertThatThrownBy(() -> service.create(request(null, 100.0)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("accountId es obligatorio");

            verifyNoInteractions(movementRepository, accountService, accountRepository);
        }

        @Test
        void propagaResourceNotFoundSiLaCuentaNoExiste() {
            when(accountService.findById(99L)).thenThrow(new ResourceNotFoundException("Cuenta no encontrada"));

            assertThatThrownBy(() -> service.create(request(99L, 100.0)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Cuenta no encontrada");

            verify(movementRepository, never()).save(any());
            verifyNoInteractions(accountRepository);
        }
    }

    @Nested
    @DisplayName("consultas")
    class Queries {

        @Test
        void findAllSinFiltroDevuelveTodos() {
            Account account = account(0.0);
            when(movementRepository.findAll()).thenReturn(List.of(
                    movement(1L, 100.0, 100.0, account), movement(2L, -50.0, 50.0, account)));

            List<MovementDTO> result = service.findAll(null);

            assertThat(result).extracting(MovementDTO::getMovementId).containsExactly(1L, 2L);
            verify(movementRepository, never()).findByAccount_AccountId(any());
        }

        @Test
        void findAllConAccountIdFiltraPorCuenta() {
            when(movementRepository.findByAccount_AccountId(1L))
                    .thenReturn(List.of(movement(1L, 100.0, 100.0, account(0.0))));

            List<MovementDTO> result = service.findAll(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAccountId()).isEqualTo(1L);
            verify(movementRepository, never()).findAll();
        }

        @Test
        void getByIdDevuelveElMovimiento() {
            when(movementRepository.findById(1L)).thenReturn(Optional.of(movement(1L, 100.0, 100.0, account(0.0))));

            MovementDTO result = service.getById(1L);

            assertThat(result.getMovementId()).isEqualTo(1L);
            assertThat(result.getMovementType()).isEqualTo("Deposito");
            assertThat(result.getBalance()).isEqualTo(100.0);
        }

        @Test
        void getByIdLanzaResourceNotFoundSiNoExiste() {
            when(movementRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Movimiento no encontrado");
        }
    }

    /**
     * Escenario base: saldo inicial 1000
     *   m1 +500 -> 1500,  m2 -200 -> 1300,  m3 +100 -> 1400  (la cuenta queda en 1400)
     */
    @Nested
    @DisplayName("update")
    class Update {

        private Account account;
        private Movement m1;
        private Movement m2;
        private Movement m3;

        @BeforeEach
        void fixture() {
            account = account(1400.0);
            m1 = movement(1L, 500.0, 1500.0, account);
            m2 = movement(2L, -200.0, 1300.0, account);
            m3 = movement(3L, 100.0, 1400.0, account);
        }

        private void stubFind(Movement m) {
            when(movementRepository.findById(m.getMovementId())).thenReturn(Optional.of(m));
        }

        private void stubChronology() {
            when(movementRepository.findByAccount_AccountIdOrderByDateAscMovementIdAsc(1L))
                    .thenReturn(List.of(m1, m2, m3));
        }

        @Test
        void recalculaElMovimientoYLosPosterioresPeroNoLosAnteriores() {
            stubFind(m2);
            stubChronology();
            MovementDTO body = MovementDTO.builder()
                    .value(-100.0).balance(99999.0).date(LocalDateTime.of(2000, 1, 1, 0, 0)).build();

            MovementDTO result = service.update(2L, body);

            assertThat(m1.getBalance()).as("anterior sin cambios").isEqualTo(1500.0);
            assertThat(m2.getValue()).isEqualTo(-100.0);
            assertThat(m2.getBalance()).isEqualTo(1400.0);
            assertThat(m3.getBalance()).isEqualTo(1500.0);
            assertThat(account.getInitialBalance()).isEqualTo(1500.0);
            assertThat(m2.getDate()).as("la fecha del body se ignora").isEqualTo(LocalDateTime.of(2024, 2, 2, 10, 0));

            verify(movementRepository).saveAll(savedMovements.capture());
            assertThat(savedMovements.getValue()).containsExactly(m2, m3);
            verify(accountRepository).save(account);

            assertThat(result.getValue()).isEqualTo(-100.0);
            assertThat(result.getBalance()).as("el saldo del body se ignora").isEqualTo(1400.0);
        }

        @Test
        void recalculaElTipoSegunElSignoDelNuevoValor() {
            stubFind(m2);
            stubChronology();

            MovementDTO result = service.update(2L, request(null, 50.0)); // de -200 a +50 => delta +250

            assertThat(result.getMovementType()).isEqualTo("Deposito");
            assertThat(m2.getBalance()).isEqualTo(1550.0);
            assertThat(m3.getBalance()).isEqualTo(1650.0);
            assertThat(account.getInitialBalance()).isEqualTo(1650.0);
        }

        @Test
        void actualizarElUltimoMovimientoSoloAfectaASiMismo() {
            stubFind(m3);
            stubChronology();

            service.update(3L, request(null, 300.0)); // delta +200

            assertThat(m1.getBalance()).isEqualTo(1500.0);
            assertThat(m2.getBalance()).isEqualTo(1300.0);
            assertThat(m3.getBalance()).isEqualTo(1600.0);
            verify(movementRepository).saveAll(savedMovements.capture());
            assertThat(savedMovements.getValue()).containsExactly(m3);
            assertThat(account.getInitialBalance()).isEqualTo(1600.0);
        }

        @Test
        void actualizarElPrimerMovimientoAfectaATodos() {
            stubFind(m1);
            stubChronology();

            service.update(1L, request(null, 100.0)); // delta -400

            assertThat(m1.getBalance()).isEqualTo(1100.0);
            assertThat(m2.getBalance()).isEqualTo(900.0);
            assertThat(m3.getBalance()).isEqualTo(1000.0);
            assertThat(account.getInitialBalance()).isEqualTo(1000.0);
        }

        @Test
        void aceptaElMismoAccountIdEnElBody() {
            stubFind(m2);
            stubChronology();

            MovementDTO result = service.update(2L, request(1L, -100.0));

            assertThat(result.getAccountId()).isEqualTo(1L);
        }

        @Test
        void rechazaSiAlgunSaldoPosteriorQuedaNegativo() {
            stubFind(m1);
            stubChronology();

            // m1 pasa de +500 a -1000 (delta -1500): m1 queda en 0 pero m2 quedaria en -200
            assertThatThrownBy(() -> service.update(1L, request(null, -1000.0)))
                    .isInstanceOf(InsufficientBalanceException.class);

            verify(movementRepository, never()).saveAll(any());
            verify(accountRepository, never()).save(any());
        }

        @Test
        void rechazaSiElPropioSaldoQuedaNegativo() {
            stubFind(m2);
            stubChronology();

            assertThatThrownBy(() -> service.update(2L, request(null, -1600.0))) // delta -1400 => -100
                    .isInstanceOf(InsufficientBalanceException.class);

            verify(movementRepository, never()).saveAll(any());
            verify(accountRepository, never()).save(any());
        }

        @Test
        void noPermiteCambiarLaCuentaDeUnMovimiento() {
            stubFind(m2);

            assertThatThrownBy(() -> service.update(2L, request(99L, -100.0)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("No se puede cambiar la cuenta de un movimiento");

            verify(movementRepository, never()).saveAll(any());
            verify(accountRepository, never()).save(any());
        }

        @ParameterizedTest(name = "valor = {0}")
        @NullSource
        @ValueSource(doubles = {0.0})
        void rechazaValorNuloOCero(Double value) {
            assertThatThrownBy(() -> service.update(2L, request(null, value)))
                    .isInstanceOf(BusinessRuleException.class);

            verifyNoInteractions(movementRepository, accountRepository);
        }

        @Test
        void lanzaResourceNotFoundSiElMovimientoNoExiste() {
            when(movementRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(99L, request(null, 100.0)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Movimiento no encontrado");

            verify(movementRepository, never()).saveAll(any());
        }
    }
}
