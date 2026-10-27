package com.devsu.cuenta.support;

import com.devsu.cuenta.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

/** Utilidades compartidas por las pruebas. */
public final class TestSupport {

    public static final ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    private TestSupport() {
    }

    /** MockMvc "standalone" (sin levantar Spring) con el GlobalExceptionHandler real. */
    public static MockMvc mockMvc(Object... controllers) {
        return MockMvcBuilders.standaloneSetup(controllers)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(MAPPER))
                .build();
    }

    public static String json(Object value) throws JsonProcessingException {
        return MAPPER.writeValueAsString(value);
    }

    /** Construye la subclase correcta de FeignException (404 -> NotFound, 503 -> ServiceUnavailable...). */
    public static FeignException feignException(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "http://localhost/clientes/1",
                Map.<String, Collection<String>>of(), null, StandardCharsets.UTF_8, null);
        Response response = Response.builder()
                .status(status)
                .reason("status " + status)
                .request(request)
                .headers(Map.<String, Collection<String>>of())
                .build();
        return FeignException.errorStatus("ClientFeign#getClientById(Long)", response);
    }
}
