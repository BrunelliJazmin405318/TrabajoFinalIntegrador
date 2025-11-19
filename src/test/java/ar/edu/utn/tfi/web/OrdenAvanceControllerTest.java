// src/test/java/ar/edu/utn/tfi/web/OrdenAvanceControllerTest.java
package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.repository.OrdenTrabajoRepository;
import ar.edu.utn.tfi.security.SecurityConfig;
import ar.edu.utn.tfi.service.OrderAdvanceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrdenAvanceController.class)
@Import(SecurityConfig.class)
@SuppressWarnings("removal")
class OrdenAvanceControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    OrderAdvanceService service;

    // aunque no se use en el controller, Spring lo va a querer crear por inyección
    @MockBean
    OrdenTrabajoRepository ordenRepo;

    @Test
    @DisplayName("POST /orden/{id}/avanzar → llama al servicio con el usuario autenticado")
    void avanzarEtapaPorId_ok() throws Exception {
        mvc.perform(post("/orden/{id}/avanzar", 42L)
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Etapa avanzada correctamente"));

        verify(service).avanzarEtapa(42L, "admin");
    }
}