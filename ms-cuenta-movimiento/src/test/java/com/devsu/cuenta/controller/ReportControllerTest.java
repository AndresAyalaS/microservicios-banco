package com.devsu.cuenta.controller;

import com.devsu.cuenta.dto.ReportDTO;
import com.devsu.cuenta.exception.ExternalServiceException;
import com.devsu.cuenta.service.ReportService;
import com.devsu.cuenta.support.TestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    private static final String URL = "/api/v1/reportes";

    @Mock
    private ReportService reportService;
    @InjectMocks
    private ReportController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = TestSupport.mockMvc(controller);
    }

    @Test
    void devuelveElReporteConTodosLosCampos() throws Exception {
        ReportDTO row = ReportDTO.builder()
                .fecha(LocalDateTime.of(2024, 2, 5, 10, 30)).cliente("Jose Lema").numeroCuenta("478758")
                .tipo("Ahorros").saldoInicial(1425.0).estado(true).movimiento(-575.0).saldoDisponible(1425.0).build();
        when(reportService.getReport(1L, LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 10))).thenReturn(List.of(row));

        mockMvc.perform(get(URL).param("clientId", "1").param("startDate", "2024-02-01").param("endDate", "2024-02-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fecha").value("2024-02-05T10:30:00"))
                .andExpect(jsonPath("$[0].cliente").value("Jose Lema"))
                .andExpect(jsonPath("$[0].numeroCuenta").value("478758"))
                .andExpect(jsonPath("$[0].tipo").value("Ahorros"))
                .andExpect(jsonPath("$[0].saldoInicial").value(1425.0))
                .andExpect(jsonPath("$[0].estado").value(true))
                .andExpect(jsonPath("$[0].movimiento").value(-575.0))
                .andExpect(jsonPath("$[0].saldoDisponible").value(1425.0));

        verify(reportService).getReport(1L, LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 10));
    }

    @Test
    void sinResultadosDevuelveArregloVacio() throws Exception {
        when(reportService.getReport(1L, LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 10))).thenReturn(List.of());

        mockMvc.perform(get(URL).param("clientId", "1").param("startDate", "2024-02-01").param("endDate", "2024-02-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void faltandoUnParametroObligatorioResponde400() throws Exception {
        mockMvc.perform(get(URL).param("clientId", "1").param("startDate", "2024-02-01"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reportService);
    }

    @Test
    void conFormatoDeFechaInvalidoResponde400() throws Exception {
        mockMvc.perform(get(URL).param("clientId", "1").param("startDate", "01/02/2024").param("endDate", "2024-02-10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Parámetro inválido: startDate"));

        verifyNoInteractions(reportService);
    }

    @Test
    void conClientIdNoNumericoResponde400() throws Exception {
        mockMvc.perform(get(URL).param("clientId", "abc").param("startDate", "2024-02-01").param("endDate", "2024-02-10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Parámetro inválido: clientId"));

        verifyNoInteractions(reportService);
    }

    @Test
    void erroresInesperadosDelServicioResponden500() throws Exception {
        when(reportService.getReport(1L, LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 10)))
                .thenThrow(new IllegalStateException("detalle interno"));

        mockMvc.perform(get(URL).param("clientId", "1").param("startDate", "2024-02-01").param("endDate", "2024-02-10"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.mensaje").value("Error interno del servidor"));
    }

    @Test
    void falloDeServicioExternoResponde503() throws Exception {
        when(reportService.getReport(1L, LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 10)))
                .thenThrow(new ExternalServiceException("Servicio de clientes no disponible", new RuntimeException()));

        mockMvc.perform(get(URL).param("clientId", "1").param("startDate", "2024-02-01").param("endDate", "2024-02-10"))
                .andExpect(status().isServiceUnavailable());
    }
}
