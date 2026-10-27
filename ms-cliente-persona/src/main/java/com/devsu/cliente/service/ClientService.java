package com.devsu.cliente.service;
import com.devsu.cliente.dto.ClientDTO;
import com.devsu.cliente.entity.Client;
import com.devsu.cliente.exception.ResourceNotFoundException;
import com.devsu.cliente.repository.ClientRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ClientService {
    private final ClientRepository repository;
    public ClientService(ClientRepository repository) { this.repository = repository; }

    public List<Client> findAll() { return repository.findAll(); }

    public Client findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
    }

    public Client create(ClientDTO dto) {
        Client client = new Client();
        mapDtoToEntity(dto, client);
        return repository.save(client);
    }

    public Client update(Long id, ClientDTO dto) {
        Client client = findById(id);
        mapDtoToEntity(dto, client);
        return repository.save(client);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Cliente no encontrado");
        }
        repository.deleteById(id);
    }

    private void mapDtoToEntity(ClientDTO dto, Client client) {
        client.setName(dto.name());
        client.setGender(dto.gender());
        client.setAge(dto.age());
        client.setDocumentId(dto.documentId());
        client.setAddress(dto.address());
        client.setPhone(dto.phone());
        client.setPassword(dto.password());
        client.setStatus(dto.status());
    }
}
