package com.devsu.cuenta.service;

import com.devsu.cuenta.dto.MovementDTO;
import com.devsu.cuenta.exception.BusinessRuleException;
import com.devsu.cuenta.exception.InsufficientBalanceException;
import com.devsu.cuenta.exception.ResourceNotFoundException;
import com.devsu.cuenta.model.Account;
import com.devsu.cuenta.model.Movement;
import com.devsu.cuenta.repository.AccountRepository;
import com.devsu.cuenta.repository.MovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MovementService {
    private static final String DEPOSIT = "Deposito";
    private static final String WITHDRAWAL = "Retiro";

    private final MovementRepository movementRepository;
    private final AccountService accountService;
    private final AccountRepository accountRepository;

    @Transactional
    public MovementDTO create(MovementDTO dto) {
        validateValue(dto.getValue());
        if (dto.getAccountId() == null) {
            throw new BusinessRuleException("accountId es obligatorio");
        }
        Account account = accountService.findById(dto.getAccountId());

        // Saldo actual = último movimiento o saldo inicial si es el primero
        double currentBalance = movementRepository
                .findTopByAccount_AccountIdOrderByDateDescMovementIdDesc(dto.getAccountId())
                .map(Movement::getBalance)
                .orElse(account.getInitialBalance());

        double newBalance = currentBalance + dto.getValue();
        if (newBalance < 0) {
            throw new InsufficientBalanceException();
        }

        // Guardar nuevo saldo en cuenta
        account.setInitialBalance(newBalance);
        accountRepository.save(account);

        Movement movement = Movement.builder()
                .date(LocalDateTime.now())
                .movementType(resolveType(dto.getValue()))
                .value(dto.getValue())
                .balance(newBalance)
                .account(account)
                .build();
        return toDto(movementRepository.save(movement));
    }

    @Transactional(readOnly = true)
    public List<MovementDTO> findAll(Long accountId) {
        List<Movement> movements = accountId == null
                ? movementRepository.findAll()
                : movementRepository.findByAccount_AccountId(accountId);
        return movements.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public MovementDTO getById(Long id) {
        return toDto(findEntityById(id));
    }

    /**
     * Actualiza el valor de un movimiento. El tipo se recalcula a partir del signo del valor
     * y se reajusta el saldo de este movimiento y de los posteriores de la misma cuenta.
     * Si algún saldo resultante queda negativo se rechaza con "Saldo no disponible".
     * La cuenta, la fecha y el saldo enviados en el body se ignoran.
     */
    @Transactional
    public MovementDTO update(Long id, MovementDTO dto) {
        validateValue(dto.getValue());
        Movement movement = findEntityById(id);
        Account account = movement.getAccount();

        if (dto.getAccountId() != null && !dto.getAccountId().equals(account.getAccountId())) {
            throw new BusinessRuleException("No se puede cambiar la cuenta de un movimiento");
        }

        double delta = dto.getValue() - movement.getValue();

        // Este movimiento y todos los posteriores de la cuenta
        List<Movement> affected = movementRepository
                .findByAccount_AccountIdOrderByDateAscMovementIdAsc(account.getAccountId())
                .stream()
                .dropWhile(m -> !m.getMovementId().equals(id))
                .toList();

        for (Movement m : affected) {
            double newBalance = m.getBalance() + delta;
            if (newBalance < 0) {
                throw new InsufficientBalanceException();
            }
            m.setBalance(newBalance);
        }

        movement.setValue(dto.getValue());
        movement.setMovementType(resolveType(dto.getValue()));
        account.setInitialBalance(account.getInitialBalance() + delta);

        movementRepository.saveAll(affected);
        accountRepository.save(account);
        return toDto(movement);
    }

    private Movement findEntityById(Long id) {
        return movementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movimiento no encontrado"));
    }

    private void validateValue(Double value) {
        if (value == null || value == 0) {
            throw new BusinessRuleException("El valor del movimiento es obligatorio y debe ser distinto de cero");
        }
    }

    private String resolveType(double value) {
        return value > 0 ? DEPOSIT : WITHDRAWAL;
    }

    private MovementDTO toDto(Movement movement) {
        return MovementDTO.builder()
                .movementId(movement.getMovementId())
                .date(movement.getDate())
                .movementType(movement.getMovementType())
                .value(movement.getValue())
                .balance(movement.getBalance())
                .accountId(movement.getAccount().getAccountId())
                .build();
    }
}