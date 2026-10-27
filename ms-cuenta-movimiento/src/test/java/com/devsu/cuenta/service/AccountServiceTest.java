package com.devsu.cuenta.service;

import com.devsu.cuenta.client.ClientFeign;
import com.devsu.cuenta.dto.AccountDTO;
import com.devsu.cuenta.exception.BusinessRuleException;
import com.devsu.cuenta.exception.DuplicateResourceException;
import com.devsu.cuenta.exception.ExternalServiceException;
import com.devsu.cuenta.exception.ResourceNotFoundException;
import com.devsu.cuenta.model.Account;
import com.devsu.cuenta.repository.AccountRepository;
import com.devsu.cuenta.support.TestSupport;
import feign.FeignException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private ClientFeign clientFeign;
    @InjectMocks
    private AccountService service;

    private static Account account() {
        return Account.builder()
                .accountId(1L)
                .accountNumber("478758")
                .accountType("Ahorros")
                .initialBalance(1000.0)
                .status(true)
                .clientId(1L)
                .build();
    }

    private static AccountDTO fullDto() {
        return AccountDTO.builder()
                .accountNumber("585545")
                .accountType("Corriente")
                .initialBalance(9999.0)
                .status(false)
                .clientId(2L)
                .build();
    }

    private void stubSaveReturnsArgument() {
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        void guardaLaCuentaCuandoElClienteExisteYElNumeroEstaLibre() {
            AccountDTO request = AccountDTO.builder()
                    .accountNumber("478758").accountType("Ahorros").initialBalance(2000.0)
                    .status(true).clientId(1L).build();
            when(accountRepository.existsByAccountNumber("478758")).thenReturn(false);
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
                Account a = inv.getArgument(0);
                a.setAccountId(10L);
                return a;
            });

            AccountDTO result = service.create(request);

            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(captor.capture());
            assertThat(captor.getValue().getAccountNumber()).isEqualTo("478758");
            assertThat(captor.getValue().getInitialBalance()).isEqualTo(2000.0);
            assertThat(captor.getValue().getClientId()).isEqualTo(1L);
            verify(clientFeign).getClientById(1L);
            assertThat(result).isEqualTo(AccountDTO.builder()
                    .accountId(10L).accountNumber("478758").accountType("Ahorros")
                    .initialBalance(2000.0).status(true).clientId(1L).build());
        }

        @Test
        void lanzaBusinessRuleExceptionSiNoSeEnviaClientId() {
            AccountDTO request = fullDto();
            request.setClientId(null);

            assertThatThrownBy(() -> service.create(request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("clientId es obligatorio");

            verifyNoInteractions(accountRepository, clientFeign);
        }

        @Test
        void lanzaResourceNotFoundSiElClienteNoExisteYNoContinua() {
            when(clientFeign.getClientById(2L)).thenThrow(TestSupport.feignException(404));

            assertThatThrownBy(() -> service.create(fullDto()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Cliente no encontrado");

            verify(accountRepository, never()).existsByAccountNumber(any());
            verify(accountRepository, never()).save(any());
        }

        @ParameterizedTest(name = "HTTP {0} del servicio de clientes")
        @ValueSource(ints = {500, 502, 503})
        void lanzaExternalServiceExceptionSiElServicioDeClientesFalla(int status) {
            FeignException feign = TestSupport.feignException(status);
            when(clientFeign.getClientById(2L)).thenThrow(feign);

            assertThatThrownBy(() -> service.create(fullDto()))
                    .isInstanceOf(ExternalServiceException.class)
                    .hasMessage("Servicio de clientes no disponible")
                    .hasCause(feign);

            verify(accountRepository, never()).save(any());
        }

        @Test
        void lanzaDuplicateResourceSiElNumeroDeCuentaYaExiste() {
            when(accountRepository.existsByAccountNumber("585545")).thenReturn(true);

            assertThatThrownBy(() -> service.create(fullDto()))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessage("Cuenta ya existe");

            verify(accountRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("consultas")
    class Queries {

        @Test
        void findAllMapeaTodasLasCuentas() {
            Account other = account();
            other.setAccountId(2L);
            other.setAccountNumber("225487");
            when(accountRepository.findAll()).thenReturn(List.of(account(), other));

            List<AccountDTO> result = service.findAll();

            assertThat(result).extracting(AccountDTO::getAccountId).containsExactly(1L, 2L);
            assertThat(result).extracting(AccountDTO::getAccountNumber).containsExactly("478758", "225487");
        }

        @Test
        void findAllDevuelveListaVaciaSiNoHayCuentas() {
            when(accountRepository.findAll()).thenReturn(List.of());

            assertThat(service.findAll()).isEmpty();
        }

        @Test
        void getByIdDevuelveLaCuenta() {
            when(accountRepository.findById(1L)).thenReturn(Optional.of(account()));

            AccountDTO result = service.getById(1L);

            assertThat(result.getAccountId()).isEqualTo(1L);
            assertThat(result.getAccountType()).isEqualTo("Ahorros");
            assertThat(result.getInitialBalance()).isEqualTo(1000.0);
            assertThat(result.getStatus()).isTrue();
            assertThat(result.getClientId()).isEqualTo(1L);
        }

        @Test
        void getByIdLanzaResourceNotFoundSiNoExiste() {
            when(accountRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Cuenta no encontrada");
        }
    }

    @Nested
    @DisplayName("update (PUT)")
    class Update {

        static Stream<AccountDTO> dtosIncompletos() {
            return Stream.of(
                    AccountDTO.builder().accountType("Ahorros").status(true).clientId(1L).build(),
                    AccountDTO.builder().accountNumber("1").status(true).clientId(1L).build(),
                    AccountDTO.builder().accountNumber("1").accountType("Ahorros").clientId(1L).build(),
                    AccountDTO.builder().accountNumber("1").accountType("Ahorros").status(true).build());
        }

        @ParameterizedTest
        @MethodSource("dtosIncompletos")
        void rechazaSiFaltaAlgunCampoObligatorio(AccountDTO incompleto) {
            assertThatThrownBy(() -> service.update(1L, incompleto))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("PUT requiere");

            verifyNoInteractions(accountRepository, clientFeign);
        }

        @Test
        void actualizaTodosLosCamposPeroIgnoraInitialBalance() {
            when(accountRepository.findById(1L)).thenReturn(Optional.of(account()));
            when(accountRepository.existsByAccountNumber("585545")).thenReturn(false);
            stubSaveReturnsArgument();

            AccountDTO result = service.update(1L, fullDto());

            assertThat(result.getAccountNumber()).isEqualTo("585545");
            assertThat(result.getAccountType()).isEqualTo("Corriente");
            assertThat(result.getStatus()).isFalse();
            assertThat(result.getClientId()).isEqualTo(2L);
            assertThat(result.getInitialBalance()).as("el saldo solo cambia con movimientos").isEqualTo(1000.0);
            verify(clientFeign).getClientById(2L);
        }

        @Test
        void noValidaNumeroNiClienteSiNoCambiaron() {
            AccountDTO same = AccountDTO.builder()
                    .accountNumber("478758").accountType("Corriente").status(false).clientId(1L).build();
            when(accountRepository.findById(1L)).thenReturn(Optional.of(account()));
            stubSaveReturnsArgument();

            AccountDTO result = service.update(1L, same);

            assertThat(result.getAccountType()).isEqualTo("Corriente");
            verify(accountRepository, never()).existsByAccountNumber(any());
            verifyNoInteractions(clientFeign);
        }

        @Test
        void lanzaDuplicateResourceSiElNuevoNumeroYaExiste() {
            when(accountRepository.findById(1L)).thenReturn(Optional.of(account()));
            when(accountRepository.existsByAccountNumber("585545")).thenReturn(true);

            assertThatThrownBy(() -> service.update(1L, fullDto()))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessage("Cuenta ya existe");

            verify(accountRepository, never()).save(any());
        }

        @Test
        void lanzaResourceNotFoundSiElNuevoClienteNoExiste() {
            when(accountRepository.findById(1L)).thenReturn(Optional.of(account()));
            when(accountRepository.existsByAccountNumber("585545")).thenReturn(false);
            when(clientFeign.getClientById(2L)).thenThrow(TestSupport.feignException(404));

            assertThatThrownBy(() -> service.update(1L, fullDto()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Cliente no encontrado");

            verify(accountRepository, never()).save(any());
        }

        @Test
        void lanzaResourceNotFoundSiLaCuentaNoExiste() {
            when(accountRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(99L, fullDto()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Cuenta no encontrada");

            verify(accountRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("partialUpdate (PATCH)")
    class PartialUpdate {

        @Test
        void soloModificaLosCamposEnviados() {
            Account existing = account();
            when(accountRepository.findById(1L)).thenReturn(Optional.of(existing));
            stubSaveReturnsArgument();

            AccountDTO result = service.partialUpdate(1L, AccountDTO.builder().status(false).build());

            assertThat(result.getStatus()).isFalse();
            assertThat(result.getAccountNumber()).isEqualTo("478758");
            assertThat(result.getAccountType()).isEqualTo("Ahorros");
            assertThat(result.getClientId()).isEqualTo(1L);
            verify(accountRepository, never()).existsByAccountNumber(any());
            verifyNoInteractions(clientFeign);
        }

        @Test
        void conDtoVacioNoCambiaNada() {
            Account existing = account();
            when(accountRepository.findById(1L)).thenReturn(Optional.of(existing));
            stubSaveReturnsArgument();

            service.partialUpdate(1L, new AccountDTO());

            assertThat(existing).isEqualTo(account());
            verifyNoInteractions(clientFeign);
        }

        @Test
        void cambiaElNumeroDeCuentaSiEstaLibre() {
            Account existing = account();
            when(accountRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(accountRepository.existsByAccountNumber("999999")).thenReturn(false);
            stubSaveReturnsArgument();

            AccountDTO result = service.partialUpdate(1L, AccountDTO.builder().accountNumber("999999").build());

            assertThat(result.getAccountNumber()).isEqualTo("999999");
        }

        @Test
        void rechazaUnNumeroDeCuentaYaUsado() {
            when(accountRepository.findById(1L)).thenReturn(Optional.of(account()));
            when(accountRepository.existsByAccountNumber("999999")).thenReturn(true);

            assertThatThrownBy(() -> service.partialUpdate(1L, AccountDTO.builder().accountNumber("999999").build()))
                    .isInstanceOf(DuplicateResourceException.class);

            verify(accountRepository, never()).save(any());
        }

        @Test
        void validaElClienteSoloSiCambia() {
            when(accountRepository.findById(1L)).thenReturn(Optional.of(account()));
            stubSaveReturnsArgument();

            AccountDTO result = service.partialUpdate(1L, AccountDTO.builder().clientId(5L).build());

            assertThat(result.getClientId()).isEqualTo(5L);
            verify(clientFeign).getClientById(5L);
        }

        @Test
        void rechazaUnClienteInexistente() {
            when(accountRepository.findById(1L)).thenReturn(Optional.of(account()));
            when(clientFeign.getClientById(5L)).thenThrow(TestSupport.feignException(404));

            assertThatThrownBy(() -> service.partialUpdate(1L, AccountDTO.builder().clientId(5L).build()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Cliente no encontrado");

            verify(accountRepository, never()).save(any());
        }

        @Test
        void lanzaResourceNotFoundSiLaCuentaNoExiste() {
            when(accountRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.partialUpdate(99L, new AccountDTO()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
