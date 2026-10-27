package com.devsu.cuenta.dto;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountDTO {
    private Long accountId;
    private String accountNumber;
    private String accountType;
    private Double initialBalance;
    private Boolean status;
    private Long clientId;
}