package com.devsu.cuenta.controller;

import com.devsu.cuenta.dto.MovementDTO;
import com.devsu.cuenta.exception.BusinessRuleException;
import com.devsu.cuenta.exception.InsufficientBalanceException;
import com.devsu.cuenta.exception.ResourceNotFoundException;
import com.devsu.cuenta.service.MovementService;
import com.devsu.cuenta.support.TestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MovementControllerTest {

    private static final String URL = "/api/v1/movimientos";

    @Mock
    private MovementService service;
    @InjectMocks
    private MovementController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = TestSupport.mockMvc(controller);
    }

    private static MovementDTO dto(Long id, double value, double balance) {
        return MovementDTO.builder().movementId(id).date(LocalDateTime.of(2024, 2, 5, 10, 30))
                .movementType(value > 0 ? "Deposito" : "Retiro").value(value).balance(balance).accountId(1L).build();
    }

    @Test
    void postRegistraElMovimientoYResponde201() throws Exception {
        when(service.create(any(MovementDTO.class))).thenReturn(dto(1L, -575.0, 1425.0));
        MovementDTO body = MovementDTO.builder().accountId(1L).value(-575.0).build();

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(TestSupport.json(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.movementId").value(1))
                .andExpect(jsonPath("$.movementType").value("Retiro"))
                .andExpect(jsonPath("$.value").value(-575.0))
                .andExpect(jsonPath("$.balance").value(1425.0))
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.date").value("2024-02-05T10:30:00"));

        ArgumentCaptor<MovementDTO> captor = ArgumentCaptor.forClass(MovementDTO.class);
        verify(service).create(captor.capture());
        assertThat(captor.getValue().getAccountId()).isEqualTo(1L);
        assertThat(captor.getValue().getValue()).isEqualTo(-575.0);
    }

    @Test
    void postConSaldoInsuficienteResponde400ConMensajeDeLaExcepcion() throws Exception {
        InsufficientBalanceException ex = new InsufficientBalanceException();
        when(service.create(any())).thenThrow(ex);

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"accountId\":1,\"value\":-9999}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(ex.getMessage()));
    }

    @Test
    void postConValorInvalidoResponde400() throws Exception {
        when(service.create(any())).thenThrow(
                new BusinessRuleException("El valor del movimiento es obligatorio y debe ser distinto de cero"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"accountId\":1,\"value\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El valor del movimiento es obligatorio y debe ser distinto de cero"));
    }

    @Test
    void postConCuentaInexistenteResponde404() throws Exception {
        when(service.create(any())).thenThrow(new ResourceNotFoundException("Cuenta no encontrada"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"accountId\":99,\"value\":10}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Cuenta no encontrada"));
    }

    @Test
    void postConValorNoNumericoResponde400SinLlamarAlServicio() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":1,\"value\":\"mucho\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Cuerpo de la solicitud inválido"));

        verifyNoInteractions(service);
    }

    @Test
    void getSinFiltroListaTodos() throws Exception {
        when(service.findAll(null)).thenReturn(List.of(dto(1L, 100.0, 100.0), dto(2L, -50.0, 50.0)));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(service).findAll(null);
    }

    @Test
    void getConAccountIdFiltraPorCuenta() throws Exception {
        when(service.findAll(1L)).thenReturn(List.of(dto(1L, 100.0, 100.0)));

        mockMvc.perform(get(URL).param("accountId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].accountId").value(1));

        verify(service).findAll(1L);
    }

    @Test
    void getConAccountIdNoNumericoResponde400() throws Exception {
        mockMvc.perform(get(URL).param("accountId", "xyz"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Parámetro inválido: accountId"));

        verifyNoInteractions(service);
    }

    @Test
    void getPorIdDevuelveElMovimiento() throws Exception {
        when(service.getById(1L)).thenReturn(dto(1L, 100.0, 100.0));

        mockMvc.perform(get(URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movementId").value(1))
                .andExpect(jsonPath("$.movementType").value("Deposito"));
    }

    @Test
    void getPorIdInexistenteResponde404() throws Exception {
        when(service.getById(99L)).thenThrow(new ResourceNotFoundException("Movimiento no encontrado"));

        mockMvc.perform(get(URL + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Movimiento no encontrado"));
    }

    @Test
    void putActualizaElMovimiento() throws Exception {
        when(service.update(eq(1L), any(MovementDTO.class))).thenReturn(dto(1L, -100.0, 1400.0));

        mockMvc.perform(put(URL + "/1").contentType(MediaType.APPLICATION_JSON).content("{\"value\":-100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(-100.0))
                .andExpect(jsonPath("$.balance").value(1400.0));

        ArgumentCaptor<MovementDTO> captor = ArgumentCaptor.forClass(MovementDTO.class);
        verify(service).update(eq(1L), captor.capture());
        assertThat(captor.getValue().getValue()).isEqualTo(-100.0);
    }

    @Test
    void putQueDejaSaldoNegativoResponde400() throws Exception {
        InsufficientBalanceException ex = new InsufficientBalanceException();
        when(service.update(eq(1L), any())).thenThrow(ex);

        mockMvc.perform(put(URL + "/1").contentType(MediaType.APPLICATION_JSON).content("{\"value\":-99999}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(ex.getMessage()));
    }

    @Test
    void putIntentandoCambiarLaCuentaResponde400() throws Exception {
        when(service.update(eq(1L), any())).thenThrow(
                new BusinessRuleException("No se puede cambiar la cuenta de un movimiento"));

        mockMvc.perform(put(URL + "/1").contentType(MediaType.APPLICATION_JSON).content("{\"accountId\":2,\"value\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("No se puede cambiar la cuenta de un movimiento"));
    }

    @Test
    void putDeMovimientoInexistenteResponde404() throws Exception {
        when(service.update(eq(99L), any())).thenThrow(new ResourceNotFoundException("Movimiento no encontrado"));

        mockMvc.perform(put(URL + "/99").contentType(MediaType.APPLICATION_JSON).content("{\"value\":10}"))
                .andExpect(status().isNotFound());
    }
}
