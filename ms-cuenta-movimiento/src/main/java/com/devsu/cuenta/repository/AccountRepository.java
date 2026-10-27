package com.devsu.cuenta.repository;
import com.devsu.cuenta.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByClientId(Long clientId);
    boolean existsByAccountNumber(String accountNumber);
}