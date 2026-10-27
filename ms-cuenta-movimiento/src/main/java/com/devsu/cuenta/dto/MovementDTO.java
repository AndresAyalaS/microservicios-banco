package com.devsu.cuenta.dto;
import lombok.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MovementDTO {
    private Long movementId;
    private LocalDateTime date;
    private String movementType;
    private Double value;
    private Double balance;
    private Long accountId;
}