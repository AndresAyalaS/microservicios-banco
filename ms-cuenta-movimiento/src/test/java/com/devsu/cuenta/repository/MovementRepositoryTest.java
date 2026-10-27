package com.devsu.cuenta.repository;

import com.devsu.cuenta.model.Account;
import com.devsu.cuenta.model.Movement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class MovementRepositoryTest {

    @Autowired
    private MovementRepository movementRepository;
    @Autowired
    private AccountRepository accountRepository;

    private Account accountA;
    private Account accountB;

    @BeforeEach
    void setUp() {
        accountA = accountRepository.save(newAccount("111", 1L));
        accountB = accountRepository.save(newAccount("222", 2L));
    }

    private static Account newAccount(String number, long clientId) {
        return Account.builder().accountNumber(number).accountType("Ahorros")
                .initialBalance(0.0).status(true).clientId(clientId).build();
    }

    private Movement save(Account account, LocalDateTime date, double value, double balance) {
        return movementRepository.save(Movement.builder().date(date).movementType(value > 0 ? "Deposito" : "Retiro")
                .value(value).balance(balance).account(account).build());
    }

    @Test
    void findByAccountIdDevuelveSoloLosMovimientosDeEsaCuenta() {
        save(accountA, LocalDateTime.of(2024, 2, 1, 10, 0), 100.0, 100.0);
        save(accountB, LocalDateTime.of(2024, 2, 1, 11, 0), 50.0, 50.0);

        List<Movement> result = movementRepository.findByAccount_AccountId(accountA.getAccountId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getValue()).isEqualTo(100.0);
    }

    @Test
    void findByDateBetweenEsInclusivoEnAmbosExtremosYFiltraPorCuenta() {
        LocalDateTime start = LocalDateTime.of(2024, 2, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 2, 10, 23, 59, 59);
        save(accountA, start.minusSeconds(1), 1.0, 1.0);          // fuera (antes)
        save(accountA, start, 2.0, 3.0);                           // limite inferior
        save(accountA, LocalDateTime.of(2024, 2, 5, 12, 0), 3.0, 6.0);
        save(accountA, end, 4.0, 10.0);                            // limite superior
        save(accountA, end.plusSeconds(1), 5.0, 15.0);            // fuera (despues)
        save(accountB, LocalDateTime.of(2024, 2, 5, 12, 0), 99.0, 99.0); // otra cuenta

        List<Movement> result = movementRepository
                .findByAccount_AccountIdAndDateBetween(accountA.getAccountId(), start, end);

        assertThat(result).extracting(Movement::getValue).containsExactlyInAnyOrder(2.0, 3.0, 4.0);
    }

    @Test
    void findTopDevuelveElMovimientoMasReciente() {
        save(accountA, LocalDateTime.of(2024, 2, 1, 10, 0), 100.0, 100.0);
        Movement latest = save(accountA, LocalDateTime.of(2024, 2, 3, 10, 0), -20.0, 80.0);
        save(accountA, LocalDateTime.of(2024, 2, 2, 10, 0), 5.0, 105.0);

        Optional<Movement> result = movementRepository
                .findTopByAccount_AccountIdOrderByDateDescMovementIdDesc(accountA.getAccountId());

        assertThat(result).map(Movement::getMovementId).contains(latest.getMovementId());
    }

    @Test
    void findTopDesempataPorIdCuandoLaFechaCoincide() {
        LocalDateTime same = LocalDateTime.of(2024, 2, 1, 10, 0);
        save(accountA, same, 100.0, 100.0);
        Movement second = save(accountA, same, -10.0, 90.0);

        Optional<Movement> result = movementRepository
                .findTopByAccount_AccountIdOrderByDateDescMovementIdDesc(accountA.getAccountId());

        assertThat(result).map(Movement::getMovementId).contains(second.getMovementId());
    }

    @Test
    void findTopSinMovimientosDevuelveVacio() {
        assertThat(movementRepository
                .findTopByAccount_AccountIdOrderByDateDescMovementIdDesc(accountA.getAccountId())).isEmpty();
    }

    @Test
    void ordenCronologicoAscendenteConDesempatePorId() {
        LocalDateTime same = LocalDateTime.of(2024, 2, 2, 10, 0);
        Movement third = save(accountA, LocalDateTime.of(2024, 2, 5, 10, 0), 1.0, 1.0);
        Movement secondA = save(accountA, same, 1.0, 1.0);
        Movement secondB = save(accountA, same, 1.0, 1.0);
        Movement first = save(accountA, LocalDateTime.of(2024, 2, 1, 10, 0), 1.0, 1.0);

        List<Movement> result = movementRepository
                .findByAccount_AccountIdOrderByDateAscMovementIdAsc(accountA.getAccountId());

        assertThat(result).extracting(Movement::getMovementId).containsExactly(
                first.getMovementId(), secondA.getMovementId(), secondB.getMovementId(), third.getMovementId());
    }
}
