package com.devsu.cuenta.service;

import com.devsu.cuenta.client.ClientFeign;
import com.devsu.cuenta.dto.AccountDTO;
import com.devsu.cuenta.exception.BusinessRuleException;
import com.devsu.cuenta.exception.DuplicateResourceException;
import com.devsu.cuenta.exception.ExternalServiceException;
import com.devsu.cuenta.exception.ResourceNotFoundException;
import com.devsu.cuenta.model.Account;
import com.devsu.cuenta.repository.AccountRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final ClientFeign clientFeign;

    public AccountDTO create(AccountDTO dto) {
        validateClientExists(dto.getClientId());
        if (accountRepository.existsByAccountNumber(dto.getAccountNumber())) {
            throw new DuplicateResourceException("Cuenta ya existe");
        }
        Account account = Account.builder()
                .accountNumber(dto.getAccountNumber())
                .accountType(dto.getAccountType())
                .initialBalance(dto.getInitialBalance())
                .status(dto.getStatus())
                .clientId(dto.getClientId())
                .build();
        return toDto(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public List<AccountDTO> findAll() {
        return accountRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public AccountDTO getById(Long id) {
        return toDto(findById(id));
    }

    /**
     * Actualización completa (PUT). Requiere accountNumber, accountType, status y clientId.
     * initialBalance se ignora: el saldo de la cuenta solo cambia a través de movimientos.
     */
    public AccountDTO update(Long id, AccountDTO dto) {
        if (dto.getAccountNumber() == null || dto.getAccountType() == null
                || dto.getStatus() == null || dto.getClientId() == null) {
            throw new BusinessRuleException("PUT requiere accountNumber, accountType, status y clientId");
        }
        return apply(findById(id), dto);
    }

    /** Actualización parcial (PATCH): solo se modifican los campos enviados. */
    public AccountDTO partialUpdate(Long id, AccountDTO dto) {
        return apply(findById(id), dto);
    }

    public Account findById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta no encontrada"));
    }

    private AccountDTO apply(Account account, AccountDTO dto) {
        if (dto.getAccountNumber() != null && !dto.getAccountNumber().equals(account.getAccountNumber())) {
            if (accountRepository.existsByAccountNumber(dto.getAccountNumber())) {
                throw new DuplicateResourceException("Cuenta ya existe");
            }
            account.setAccountNumber(dto.getAccountNumber());
        }
        if (dto.getAccountType() != null) {
            account.setAccountType(dto.getAccountType());
        }
        if (dto.getStatus() != null) {
            account.setStatus(dto.getStatus());
        }
        if (dto.getClientId() != null && !dto.getClientId().equals(account.getClientId())) {
            validateClientExists(dto.getClientId());
            account.setClientId(dto.getClientId());
        }
        return toDto(accountRepository.save(account));
    }

    private void validateClientExists(Long clientId) {
        if (clientId == null) {
            throw new BusinessRuleException("clientId es obligatorio");
        }
        try {
            clientFeign.getClientById(clientId);
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Cliente no encontrado");
        } catch (FeignException e) {
            throw new ExternalServiceException("Servicio de clientes no disponible", e);
        }
    }

    private AccountDTO toDto(Account account) {
        return AccountDTO.builder()
                .accountId(account.getAccountId())
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType())
                .initialBalance(account.getInitialBalance())
                .status(account.getStatus())
                .clientId(account.getClientId())
                .build();
    }
}