package com.devsu.cuenta.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long movementId;
    private LocalDateTime date;
    private String movementType; // Deposito, Retiro
    @Column(name = "movement_value")
    private Double value;
    private Double balance; // saldo disponible después del movimiento

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;
}