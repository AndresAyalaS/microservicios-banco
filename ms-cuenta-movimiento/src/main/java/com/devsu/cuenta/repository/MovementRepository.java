package com.devsu.cuenta.repository;
import com.devsu.cuenta.model.Movement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MovementRepository extends JpaRepository<Movement, Long> {
    List<Movement> findByAccount_AccountIdAndDateBetween(Long accountId, LocalDateTime start, LocalDateTime end);
    List<Movement> findByAccount_AccountId(Long accountId);
    // Último movimiento de la cuenta (desempata por id si coinciden en fecha)
    Optional<Movement> findTopByAccount_AccountIdOrderByDateDescMovementIdDesc(Long accountId);
    // Todos los movimientos de la cuenta en orden cronológico (usado al recalcular saldos)
    List<Movement> findByAccount_AccountIdOrderByDateAscMovementIdAsc(Long accountId);
}
