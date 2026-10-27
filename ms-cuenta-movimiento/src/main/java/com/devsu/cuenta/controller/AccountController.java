package com.devsu.cuenta.controller;

import com.devsu.cuenta.dto.AccountDTO;
import com.devsu.cuenta.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cuentas")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountDTO create(@RequestBody AccountDTO dto) {
        return service.create(dto);
    }

    @GetMapping
    public List<AccountDTO> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public AccountDTO getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public AccountDTO update(@PathVariable Long id, @RequestBody AccountDTO dto) {
        return service.update(id, dto);
    }

    @PatchMapping("/{id}")
    public AccountDTO partialUpdate(@PathVariable Long id, @RequestBody AccountDTO dto) {
        return service.partialUpdate(id, dto);
    }
}