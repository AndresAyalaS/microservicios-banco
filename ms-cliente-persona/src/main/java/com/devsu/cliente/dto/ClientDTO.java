package com.devsu.cliente.dto;

public record ClientDTO(
        Long clientId,
        String name,
        String gender,
        Integer age,
        String documentId,
        String address,
        String phone,
        String password,
        Boolean status
) {}
