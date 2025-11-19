// src/test/java/ar/edu/utn/tfi/web/IngresosReportControllerTest.java
package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.security.SecurityConfig;
import ar.edu.utn.tfi.service.IngresosReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IngresosReportController.class)
@Import(SecurityConfig.class)
@SuppressWarnings("removal")
class IngresosReportControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    IngresosReportService service;

    @Test
    @DisplayName("GET /api/reportes/ingresos-senas-vs-finales con from/to → delega a service y devuelve totalGeneral")
    void ingresos_conFechas_ok() throws Exception {
        Map<String, Object> fake = Map.of(
                "labels", List.of("2025-01"),
                "senas", List.of(BigDecimal.TEN),
                "finales", List.of(BigDecimal.ZERO),
                "totalGeneral", BigDecimal.TEN
        );

        when(service.ingresosSenasVsFinales(
                LocalDate.parse("2025-01-01"),
                LocalDate.parse("2025-03-31")
        )).thenReturn(fake);

        mvc.perform(get("/api/reportes/ingresos-senas-vs-finales")
                        .param("from", "2025-01-01")
                        .param("to", "2025-03-31")
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalGeneral").value(10.0));

        verify(service).ingresosSenasVsFinales(
                LocalDate.parse("2025-01-01"),
                LocalDate.parse("2025-03-31")
        );
    }

    @Test
    @DisplayName("GET /api/reportes/ingresos-senas-vs-finales sin params → llama service con null, null")
    void ingresos_sinFechas_ok() throws Exception {
        when(service.ingresosSenasVsFinales(null, null))
                .thenReturn(Map.of("totalGeneral", BigDecimal.ZERO));

        mvc.perform(get("/api/reportes/ingresos-senas-vs-finales")
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalGeneral").value(0.0));

        verify(service).ingresosSenasVsFinales(null, null);
    }
}