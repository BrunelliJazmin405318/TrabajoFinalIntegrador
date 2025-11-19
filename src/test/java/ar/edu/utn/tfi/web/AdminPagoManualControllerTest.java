// src/test/java/ar/edu/utn/tfi/web/AdminPagoManualControllerTest.java
package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.domain.PagoManual;
import ar.edu.utn.tfi.domain.Presupuesto;
import ar.edu.utn.tfi.security.SecurityConfig;
import ar.edu.utn.tfi.service.PresupuestoGestionService;
import ar.edu.utn.tfi.web.dto.PagoManualReq;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminPagoManualController.class)
@Import(SecurityConfig.class)
@SuppressWarnings("removal")
class AdminPagoManualControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    PresupuestoGestionService service;

    @Test
    @DisplayName("POST /admin/pagos-manuales/registrar → registra pago manual y devuelve datos básicos")
    void registrar_ok() throws Exception {
        // Arrange: armo un Presupuesto y un PagoManual dummy
        Presupuesto p = new Presupuesto();
        p.setId(99L);

        PagoManual pm = new PagoManual();
        pm.setId(1L);
        pm.setPresupuesto(p);
        pm.setTipo("SENA");

        when(service.registrarPagoManual(any(PagoManualReq.class), eq("admin")))
                .thenReturn(pm);

        String bodyJson = """
                {
                  "presupuestoId": 99,
                  "tipo": "SENA",
                  "monto": 15000.0
                }
                """;

        // Act + Assert
        mvc.perform(post("/admin/pagos-manuales/registrar")
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.presupuestoId").value(99L))
                .andExpect(jsonPath("$.tipo").value("SENA"));

        verify(service).registrarPagoManual(any(PagoManualReq.class), eq("admin"));
    }
}