package com.devsu.cuenta.repository;

import com.devsu.cuenta.model.Account;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class AccountRepositoryTest {

    @Autowired
    private AccountRepository repository;

    private Account account(String number, long clientId) {
        return Account.builder().accountNumber(number).accountType("Ahorros")
                .initialBalance(100.0).status(true).clientId(clientId).build();
    }

    @Test
    void findByClientIdDevuelveSoloLasCuentasDelCliente() {
        repository.save(account("111", 1L));
        repository.save(account("222", 1L));
        repository.save(account("333", 2L));

        List<Account> result = repository.findByClientId(1L);

        assertThat(result).extracting(Account::getAccountNumber).containsExactlyInAnyOrder("111", "222");
    }

    @Test
    void findByClientIdSinCuentasDevuelveVacio() {
        assertThat(repository.findByClientId(404L)).isEmpty();
    }

    @Test
    void existsByAccountNumber() {
        repository.save(account("111", 1L));

        assertThat(repository.existsByAccountNumber("111")).isTrue();
        assertThat(repository.existsByAccountNumber("999")).isFalse();
    }

    @Test
    void elNumeroDeCuentaEsUnico() {
        repository.saveAndFlush(account("111", 1L));

        assertThatThrownBy(() -> repository.saveAndFlush(account("111", 2L)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
