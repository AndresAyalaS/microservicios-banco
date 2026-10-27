package com.devsu.cuenta.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Account {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accountId;
    @Column(unique = true)
    private String accountNumber;
    private String accountType; // Ahorros, Corriente
    private Double initialBalance;
    private Boolean status;
    private Long clientId;
}
