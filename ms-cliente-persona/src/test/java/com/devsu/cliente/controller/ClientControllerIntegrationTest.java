package com.devsu.cliente.controller;

import com.devsu.cliente.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración: recorre controller -> service -> repositorio -> base de datos
 * (H2 en memoria, según application.properties).
 */
@SpringBootTest
class ClientControllerIntegrationTest {

    private static final String BASE_URL = "/api/v1/clients";

    private static final String CLIENT_JSON = """
            {
              "name": "Jose Lema",
              "gender": "M",
              "age": 30,
              "documentId": "1710000001",
              "address": "Otavalo sn y principal",
              "phone": "098254785",
              "password": "1234",
              "status": true
            }
            """;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ClientRepository repository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        repository.deleteAll();
    }

    @Test
    @DisplayName("POST crea el cliente (201), lo persiste y GET por id lo devuelve")
    void crearYConsultarCliente() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CLIENT_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").isNumber())
                .andExpect(jsonPath("$.name").value("Jose Lema"));

        assertEquals(1, repository.count());
        Long id = repository.findAll().get(0).getClientId();

        mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value("1710000001"))
                .andExpect(jsonPath("$.address").value("Otavalo sn y principal"));
    }

    @Test
    @DisplayName("GET de un cliente inexistente responde 404 con mensaje")
    void consultarClienteInexistenteDebeResponder404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Cliente no encontrado"));
    }

    @Test
    @DisplayName("POST con identificación duplicada responde 409")
    void crearClienteConIdentificacionDuplicadaDebeResponder409() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CLIENT_JSON))
                .andExpect(status().isCreated());

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CLIENT_JSON))
                .andExpect(status().isConflict());

        assertEquals(1, repository.count());
    }

    @Test
    @DisplayName("DELETE elimina el cliente (204) y luego ya no existe (404)")
    void eliminarCliente() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CLIENT_JSON))
                .andExpect(status().isCreated());
        Long id = repository.findAll().get(0).getClientId();

        mockMvc.perform(delete(BASE_URL + "/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(status().isNotFound());
    }
}