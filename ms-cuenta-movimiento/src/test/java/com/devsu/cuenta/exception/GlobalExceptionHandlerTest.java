package com.devsu.cuenta.exception;

import com.devsu.cuenta.support.TestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    /** Controller de apoyo: lanza la excepcion que cada prueba configure. */
    @RestController
    static class ThrowingController {
        RuntimeException toThrow;

        @GetMapping("/boom")
        public String boom() {
            throw toThrow;
        }

        @GetMapping("/typed/{id}")
        public String typed(@org.springframework.web.bind.annotation.PathVariable Long id) {
            return "ok";
        }
    }

    private ThrowingController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new ThrowingController();
        mockMvc = TestSupport.mockMvc(controller);
    }

    static Stream<Arguments> excepciones() {
        return Stream.of(
                Arguments.of(new ResourceNotFoundException("Cuenta no encontrada"), 404, "Cuenta no encontrada"),
                Arguments.of(new DuplicateResourceException("Cuenta ya existe"), 409, "Cuenta ya existe"),
                Arguments.of(new BusinessRuleException("regla de negocio"), 400, "regla de negocio"),
                Arguments.of(new InsufficientBalanceException(), 400, new InsufficientBalanceException().getMessage()),
                Arguments.of(new ExternalServiceException("Servicio no disponible", new RuntimeException()), 503,
                        "Servicio no disponible"),
                Arguments.of(new IllegalStateException("detalle"), 500, "Error interno del servidor"),
                Arguments.of(new NullPointerException(), 500, "Error interno del servidor"));
    }

    @ParameterizedTest(name = "{0} -> HTTP {1}")
    @MethodSource("excepciones")
    void mapeaCadaExcepcionAlCodigoYMensajeEsperado(RuntimeException ex, int expectedStatus, String expectedMessage)
            throws Exception {
        controller.toThrow = ex;

        mockMvc.perform(get("/boom"))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.mensaje").value(expectedMessage));
    }

    @Test
    void elSaldoInsuficienteEsUnaReglaDeNegocio() {
        assertThat(new InsufficientBalanceException()).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void elErrorInternoNoExponeElDetalleDeLaExcepcion() throws Exception {
        controller.toThrow = new IllegalStateException("password=secreto");

        mockMvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(not(containsString("secreto"))));
    }

    @Test
    void parametroDeRutaConTipoInvalidoResponde400ConElNombreDelParametro() throws Exception {
        mockMvc.perform(get("/typed/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Parámetro inválido: id"));
    }
}
