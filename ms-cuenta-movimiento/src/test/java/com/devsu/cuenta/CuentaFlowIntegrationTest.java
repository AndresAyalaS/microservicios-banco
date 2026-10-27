package com.devsu.cuenta;

import com.devsu.cuenta.client.ClientDTO;
import com.devsu.cuenta.client.ClientFeign;
import com.devsu.cuenta.client.ClientService;
import com.devsu.cuenta.support.TestSupport;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo completo (controller -> service -> repository -> H2) con los servicios de clientes simulados.
 * Cada prueba corre en una transaccion que se revierte al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CuentaFlowIntegrationTest {

    private static final String ACCOUNT_JSON = """
            {"accountNumber":"478758","accountType":"Ahorros","initialBalance":2000.0,"status":true,"clientId":1}
            """;

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private ClientFeign clientFeign;
    @MockBean
    private ClientService clientService;

    private long createAccount() throws Exception {
        String body = mockMvc.perform(post("/api/v1/cuentas").contentType(MediaType.APPLICATION_JSON).content(ACCOUNT_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.accountId")).longValue();
    }

    private void movement(long accountId, double value) throws Exception {
        mockMvc.perform(post("/api/v1/movimientos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":" + accountId + ",\"value\":" + value + "}"))
                .andExpect(status().isCreated());
    }

    @Test
    void flujoCompletoDeCuentaMovimientosYReporte() throws Exception {
        ClientDTO client = new ClientDTO();
        client.setId(1L);
        client.setName("Jose Lema");
        when(clientService.findById(1L)).thenReturn(client);

        long accountId = createAccount();

        mockMvc.perform(post("/api/v1/movimientos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":" + accountId + ",\"value\":-575}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.movementType").value("Retiro"))
                .andExpect(jsonPath("$.balance").value(1425.0));

        mockMvc.perform(post("/api/v1/movimientos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":" + accountId + ",\"value\":600}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.movementType").value("Deposito"))
                .andExpect(jsonPath("$.balance").value(2025.0));

        mockMvc.perform(post("/api/v1/movimientos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":" + accountId + ",\"value\":-5000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").exists());

        mockMvc.perform(get("/api/v1/movimientos").param("accountId", String.valueOf(accountId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        LocalDate today = LocalDate.now();
        mockMvc.perform(get("/api/v1/reportes")
                        .param("clientId", "1")
                        .param("startDate", today.minusDays(1).toString())
                        .param("endDate", today.plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].cliente").value(containsInAnyOrder("Jose Lema", "Jose Lema")))
                .andExpect(jsonPath("$[*].numeroCuenta").value(containsInAnyOrder("478758", "478758")))
                .andExpect(jsonPath("$[*].movimiento").value(containsInAnyOrder(-575.0, 600.0)));
    }

    @Test
    void actualizarUnMovimientoRecalculaLosSaldosPosteriores() throws Exception {
        long accountId = createAccount();
        movement(accountId, -500);   // 1500
        movement(accountId, 100);    // 1600

        String body = mockMvc.perform(get("/api/v1/movimientos").param("accountId", String.valueOf(accountId)))
                .andReturn().getResponse().getContentAsString();
        long firstId = ((Number) JsonPath.read(body, "$[0].movementId")).longValue();

        mockMvc.perform(put("/api/v1/movimientos/" + firstId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":-300}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1700.0));

        mockMvc.perform(get("/api/v1/cuentas/" + accountId))
                .andExpect(jsonPath("$.initialBalance").value(1800.0));
    }

    @Test
    void crearCuentaDuplicadaResponde409() throws Exception {
        createAccount();

        mockMvc.perform(post("/api/v1/cuentas").contentType(MediaType.APPLICATION_JSON).content(ACCOUNT_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Cuenta ya existe"));
    }

    @Test
    void crearCuentaParaClienteInexistenteResponde404() throws Exception {
        when(clientFeign.getClientById(1L)).thenThrow(TestSupport.feignException(404));

        mockMvc.perform(post("/api/v1/cuentas").contentType(MediaType.APPLICATION_JSON).content(ACCOUNT_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Cliente no encontrado"));
    }

    @Test
    void crearCuentaConServicioDeClientesCaidoResponde503() throws Exception {
        when(clientFeign.getClientById(1L)).thenThrow(TestSupport.feignException(503));

        mockMvc.perform(post("/api/v1/cuentas").contentType(MediaType.APPLICATION_JSON).content(ACCOUNT_JSON))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void movimientoEnCuentaInexistenteResponde404() throws Exception {
        mockMvc.perform(post("/api/v1/movimientos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":987654,\"value\":10}"))
                .andExpect(status().isNotFound());
    }
}
