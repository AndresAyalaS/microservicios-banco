package com.devsu.cliente.controller;
import com.devsu.cliente.dto.ClientDTO;
import com.devsu.cliente.entity.Client;
import com.devsu.cliente.service.ClientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {
    private final ClientService service;
    public ClientController(ClientService service) { this.service = service; }

    @GetMapping public List<Client> getAll() { return service.findAll(); }
    @GetMapping("/{id}") public Client getById(@PathVariable Long id) { return service.findById(id); }
    @PostMapping public ResponseEntity<Client> create(@RequestBody ClientDTO dto) { return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto)); }
    @PutMapping("/{id}") public Client update(@PathVariable Long id, @RequestBody ClientDTO dto) { return service.update(id, dto); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }
}
