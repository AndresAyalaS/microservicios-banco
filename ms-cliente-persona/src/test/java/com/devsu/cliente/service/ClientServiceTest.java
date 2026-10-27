package com.devsu.cliente.service;

import com.devsu.cliente.dto.ClientDTO;
import com.devsu.cliente.entity.Client;
import com.devsu.cliente.exception.ResourceNotFoundException;
import com.devsu.cliente.repository.ClientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository repository;

    @InjectMocks
    private ClientService service;

    private ClientDTO joseDto() {
        return new ClientDTO(null, "Jose Lema", "M", 30, "1710000001",
                "Otavalo sn y principal", "098254785", "1234", true);
    }

    private Client existingClient() {
        Client client = new Client();
        client.setClientId(1L);
        client.setName("Jose Lema");
        client.setGender("M");
        client.setAge(30);
        client.setDocumentId("1710000001");
        client.setAddress("Otavalo sn y principal");
        client.setPhone("098254785");
        client.setPassword("1234");
        client.setStatus(true);
        return client;
    }

    @Test
    @DisplayName("create mapea el DTO a la entidad y la guarda")
    void createDebeMapearDtoYGuardar() {
        when(repository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        Client result = service.create(joseDto());

        ArgumentCaptor<Client> captor = ArgumentCaptor.forClass(Client.class);
        verify(repository).save(captor.capture());
        Client saved = captor.getValue();
        assertEquals("Jose Lema", saved.getName());
        assertEquals("M", saved.getGender());
        assertEquals(30, saved.getAge());
        assertEquals("1710000001", saved.getDocumentId());
        assertEquals("Otavalo sn y principal", saved.getAddress());
        assertEquals("098254785", saved.getPhone());
        assertEquals("1234", saved.getPassword());
        assertTrue(saved.getStatus());
        assertSame(saved, result);
    }

    @Test
    @DisplayName("findAll devuelve todos los clientes del repositorio")
    void findAllDebeRetornarClientes() {
        when(repository.findAll()).thenReturn(List.of(existingClient(), new Client()));

        assertEquals(2, service.findAll().size());
    }

    @Test
    @DisplayName("findById devuelve el cliente cuando existe")
    void findByIdDebeRetornarClienteExistente() {
        when(repository.findById(1L)).thenReturn(Optional.of(existingClient()));

        Client result = service.findById(1L);

        assertEquals(1L, result.getClientId());
        assertEquals("Jose Lema", result.getName());
    }

    @Test
    @DisplayName("findById lanza ResourceNotFoundException cuando no existe")
    void findByIdCuandoNoExisteDebeLanzarExcepcion() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex =
                assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));

        assertEquals("Cliente no encontrado", ex.getMessage());
    }

    @Test
    @DisplayName("update modifica los campos del cliente existente y lo guarda")
    void updateDebeActualizarCampos() {
        when(repository.findById(1L)).thenReturn(Optional.of(existingClient()));
        when(repository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));
        ClientDTO changes = new ClientDTO(1L, "Jose Lema", "M", 31, "1710000001",
                "Nueva direccion 123", "099999999", "1234", false);

        Client result = service.update(1L, changes);

        assertEquals(1L, result.getClientId());
        assertEquals(31, result.getAge());
        assertEquals("Nueva direccion 123", result.getAddress());
        assertEquals("099999999", result.getPhone());
        assertFalse(result.getStatus());
        verify(repository).save(result);
    }

    @Test
    @DisplayName("update no guarda nada si el cliente no existe")
    void updateCuandoNoExisteDebeLanzarExcepcionYNoGuardar() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(99L, joseDto()));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("delete elimina el cliente cuando existe")
    void deleteDebeEliminarClienteExistente() {
        when(repository.existsById(1L)).thenReturn(true);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    @DisplayName("delete lanza ResourceNotFoundException si el cliente no existe")
    void deleteCuandoNoExisteDebeLanzarExcepcion() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.delete(99L));

        verify(repository, never()).deleteById(anyLong());
    }
}
