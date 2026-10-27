package com.devsu.cuenta.service;

import com.devsu.cuenta.client.ClientService;
import com.devsu.cuenta.dto.ReportDTO;
import com.devsu.cuenta.model.Account;
import com.devsu.cuenta.model.Movement;
import com.devsu.cuenta.repository.AccountRepository;
import com.devsu.cuenta.repository.MovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final AccountRepository accountRepository;
    private final MovementRepository movementRepository;
    private final ClientService clientService;

    public List<ReportDTO> getReport(Long clientId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        // Nombre del cliente desde el micro 8080
        String clientName;
        try {
            clientName = clientService.findById(clientId).getName(); // si tu dto tiene getName()
        } catch (Exception e) {
            clientName = "Cliente " + clientId;
        }

        List<Account> accounts = accountRepository.findByClientId(clientId);
        List<ReportDTO> report = new ArrayList<>();

        for (Account acc : accounts) {
            List<Movement> movs = movementRepository
                    .findByAccount_AccountIdAndDateBetween(acc.getAccountId(), start, end);

            for (Movement m : movs) {
                report.add(ReportDTO.builder()
                        .fecha(m.getDate())
                        .cliente(clientName)
                        .numeroCuenta(acc.getAccountNumber())
                        .tipo(acc.getAccountType())
                        .saldoInicial(acc.getInitialBalance())
                        .estado(acc.getStatus())
                        .movimiento(m.getValue())
                        .saldoDisponible(m.getBalance())
                        .build());
            }
        }
        return report;
    }
}