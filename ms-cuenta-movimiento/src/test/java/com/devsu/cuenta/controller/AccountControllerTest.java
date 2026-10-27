package com.devsu.cuenta.controller;

import com.devsu.cuenta.dto.AccountDTO;
import com.devsu.cuenta.exception.BusinessRuleException;
import com.devsu.cuenta.exception.DuplicateResourceException;
import com.devsu.cuenta.exception.ExternalServiceException;
import com.devsu.cuenta.exception.ResourceNotFoundException;
import com.devsu.cuenta.service.AccountService;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    private static final String URL = "/api/v1/cuentas";

    @Mock
    private AccountService service;
    @InjectMocks
    private AccountController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = TestSupport.mockMvc(controller);
    }

    private static AccountDTO dto(Long id) {
        return AccountDTO.builder().accountId(id).accountNumber("478758").accountType("Ahorros")
                .initialBalance(2000.0).status(true).clientId(1L).build();
    }

    @Test
    void postCreaLaCuentaYResponde201() throws Exception {
        when(service.create(any(AccountDTO.class))).thenReturn(dto(1L));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(TestSupport.json(dto(null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.accountNumber").value("478758"))
                .andExpect(jsonPath("$.accountType").value("Ahorros"))
                .andExpect(jsonPath("$.initialBalance").value(2000.0))
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.clientId").value(1));

        ArgumentCaptor<AccountDTO> captor = ArgumentCaptor.forClass(AccountDTO.class);
        verify(service).create(captor.capture());
        assertThat(captor.getValue().getAccountNumber()).isEqualTo("478758");
        assertThat(captor.getValue().getClientId()).isEqualTo(1L);
    }

    @Test
    void postConCuentaDuplicadaResponde409() throws Exception {
        when(service.create(any())).thenThrow(new DuplicateResourceException("Cuenta ya existe"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(TestSupport.json(dto(null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Cuenta ya existe"));
    }

    @Test
    void postConClienteInexistenteResponde404() throws Exception {
        when(service.create(any())).thenThrow(new ResourceNotFoundException("Cliente no encontrado"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(TestSupport.json(dto(null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Cliente no encontrado"));
    }

    @Test
    void postConServicioDeClientesCaidoResponde503() throws Exception {
        when(service.create(any())).thenThrow(
                new ExternalServiceException("Servicio de clientes no disponible", new RuntimeException("boom")));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(TestSupport.json(dto(null))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensaje").value("Servicio de clientes no disponible"));
    }

    @Test
    void postSinClientIdResponde400() throws Exception {
        when(service.create(any())).thenThrow(new BusinessRuleException("clientId es obligatorio"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("clientId es obligatorio"));
    }

    @Test
    void postConJsonMalformadoResponde400SinLlamarAlServicio() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Cuerpo de la solicitud inválido"));

        verifyNoInteractions(service);
    }

    @Test
    void getListaTodasLasCuentas() throws Exception {
        when(service.findAll()).thenReturn(List.of(dto(1L), dto(2L)));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].accountId").value(1))
                .andExpect(jsonPath("$[1].accountId").value(2));
    }

    @Test
    void getSinCuentasDevuelveArregloVacio() throws Exception {
        when(service.findAll()).thenReturn(List.of());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getPorIdDevuelveLaCuenta() throws Exception {
        when(service.getById(1L)).thenReturn(dto(1L));

        mockMvc.perform(get(URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.accountNumber").value("478758"));
    }

    @Test
    void getPorIdInexistenteResponde404() throws Exception {
        when(service.getById(99L)).thenThrow(new ResourceNotFoundException("Cuenta no encontrada"));

        mockMvc.perform(get(URL + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Cuenta no encontrada"));
    }

    @Test
    void getConIdNoNumericoResponde400() throws Exception {
        mockMvc.perform(get(URL + "/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Parámetro inválido: id"));

        verifyNoInteractions(service);
    }

    @Test
    void putActualizaLaCuenta() throws Exception {
        AccountDTO updated = dto(1L);
        updated.setStatus(false);
        when(service.update(eq(1L), any(AccountDTO.class))).thenReturn(updated);

        mockMvc.perform(put(URL + "/1").contentType(MediaType.APPLICATION_JSON).content(TestSupport.json(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(false));

        ArgumentCaptor<AccountDTO> captor = ArgumentCaptor.forClass(AccountDTO.class);
        verify(service).update(eq(1L), captor.capture());
        assertThat(captor.getValue().getStatus()).isFalse();
    }

    @Test
    void putIncompletoResponde400() throws Exception {
        when(service.update(eq(1L), any())).thenThrow(
                new BusinessRuleException("PUT requiere accountNumber, accountType, status y clientId"));

        mockMvc.perform(put(URL + "/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("PUT requiere accountNumber, accountType, status y clientId"));
    }

    @Test
    void patchActualizaParcialmente() throws Exception {
        AccountDTO updated = dto(1L);
        updated.setAccountType("Corriente");
        when(service.partialUpdate(eq(1L), any(AccountDTO.class))).thenReturn(updated);

        mockMvc.perform(patch(URL + "/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountType\":\"Corriente\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountType").value("Corriente"));

        ArgumentCaptor<AccountDTO> captor = ArgumentCaptor.forClass(AccountDTO.class);
        verify(service).partialUpdate(eq(1L), captor.capture());
        assertThat(captor.getValue().getAccountType()).isEqualTo("Corriente");
        assertThat(captor.getValue().getAccountNumber()).isNull();
    }

    @Test
    void patchDeCuentaInexistenteResponde404() throws Exception {
        when(service.partialUpdate(eq(99L), any())).thenThrow(new ResourceNotFoundException("Cuenta no encontrada"));

        mockMvc.perform(patch(URL + "/99").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }
}
