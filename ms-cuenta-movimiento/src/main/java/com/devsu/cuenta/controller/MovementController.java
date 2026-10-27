package com.devsu.cuenta.controller;

import com.devsu.cuenta.dto.MovementDTO;
import com.devsu.cuenta.service.MovementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/movimientos")
@RequiredArgsConstructor
public class MovementController {
    private final MovementService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovementDTO create(@RequestBody MovementDTO dto) {
        return service.create(dto);
    }

    // Filtro opcional: /movimientos?accountId=1
    @GetMapping
    public List<MovementDTO> getAll(@RequestParam(required = false) Long accountId) {
        return service.findAll(accountId);
    }

    @GetMapping("/{id}")
    public MovementDTO getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public MovementDTO update(@PathVariable Long id, @RequestBody MovementDTO dto) {
        return service.update(id, dto);
    }
}