package com.devsu.cliente;

import com.devsu.cliente.entity.Client;
import com.devsu.cliente.entity.Person;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ClientUnitTest {

    @Test
    @DisplayName("Un cliente es una persona: hereda sus datos personales")
    void clienteDebeHeredarDePersona() {
        assertInstanceOf(Person.class, new Client());
    }

    @Test
    @DisplayName("Un cliente nuevo no tiene id, estado ni datos asignados")
    void clienteNuevoNoDebeTenerDatosAsignados() {
        Client client = new Client();

        assertNull(client.getClientId());
        assertNull(client.getStatus());
        assertNull(client.getPassword());
        assertNull(client.getName());
        assertNull(client.getDocumentId());
    }

    @Test
    @DisplayName("Los datos de persona y de cliente se almacenan juntos")
    void debeAlmacenarDatosDePersonaYDeCliente() {
        Client client = new Client();
        client.setClientId(1L);
        client.setName("Jose Lema");
        client.setGender("M");
        client.setAge(30);
        client.setDocumentId("1234567890");
        client.setAddress("Otavalo sn y principal");
        client.setPhone("098254785");
        client.setPassword("1234");
        client.setStatus(true);

        assertEquals(1L, client.getClientId());
        assertEquals("Jose Lema", client.getName());
        assertEquals("M", client.getGender());
        assertEquals(30, client.getAge());
        assertEquals("1234567890", client.getDocumentId());
        assertEquals("Otavalo sn y principal", client.getAddress());
        assertEquals("098254785", client.getPhone());
        assertEquals("1234", client.getPassword());
        assertTrue(client.getStatus());
    }

    @Test
    @DisplayName("Desactivar un cliente no altera sus datos personales")
    void desactivarClienteNoDebeAlterarDatosPersonales() {
        Client client = new Client();
        client.setName("Marianela Montalvo");
        client.setDocumentId("1710000002");
        client.setStatus(true);

        client.setStatus(false);

        assertFalse(client.getStatus());
        assertEquals("Marianela Montalvo", client.getName());
        assertEquals("1710000002", client.getDocumentId());
    }
}