package com.devsu.cuenta.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class ReportDTO {
    private LocalDateTime fecha;
    private String cliente;
    private String numeroCuenta;
    private String tipo;
    private Double saldoInicial;
    private Boolean estado;
    private Double movimiento;
    private Double saldoDisponible;
}