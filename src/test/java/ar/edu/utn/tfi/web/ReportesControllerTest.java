// src/test/java/ar/edu/utn/tfi/web/ReportesControllerTest.java
package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.repository.OrdenTrabajoReportRepository;
import ar.edu.utn.tfi.security.SecurityConfig;
import ar.edu.utn.tfi.service.ReportesService;
import ar.edu.utn.tfi.web.dto.ClienteRankingDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportesController.class)
@Import(SecurityConfig.class)          // 👈 usamos la config real
@SuppressWarnings("removal")
class ReportesControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    ReportesService service;

    @MockBean
    OrdenTrabajoReportRepository reportRepo;

    @Test
    @DisplayName("GET /api/reportes/motores-vs-tapas → llama a service.motoresVsTapas con fechas dadas")
    void motoresVsTapas_ok() throws Exception {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to   = LocalDate.of(2025, 1, 31);

        when(service.motoresVsTapas(from, to))
                .thenReturn(Map.of("motores", 10, "tapas", 5));

        mvc.perform(get("/api/reportes/motores-vs-tapas")
                        .param("from", "2025-01-01")
                        .param("to", "2025-01-31")
                        .with(httpBasic("admin", "admin"))          // 👈 auth
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motores").value(10))
                .andExpect(jsonPath("$.tapas").value(5));

        verify(service).motoresVsTapas(from, to);
    }

    @Test
    @DisplayName("GET /api/reportes/motores-por-etapa → delega en service.motoresPorEtapa")
    void motoresPorEtapa_ok() throws Exception {
        LocalDate from = LocalDate.of(2025, 2, 1);
        LocalDate to   = LocalDate.of(2025, 2, 28);

        when(service.motoresPorEtapa(from, to))
                .thenReturn(Map.of("etapas", List.of("INGRESO"), "cantidades", List.of(3)));

        mvc.perform(get("/api/reportes/motores-por-etapa")
                        .param("from", "2025-02-01")
                        .param("to", "2025-02-28")
                        .with(httpBasic("admin", "admin"))          // 👈 auth
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapas[0]").value("INGRESO"))
                .andExpect(jsonPath("$.cantidades[0]").value(3));

        verify(service).motoresPorEtapa(from, to);
    }

    @Test
    @DisplayName("GET /api/reportes/clientes-frecuentes → devuelve lista de ClienteRankingDTO")
    void clientesFrecuentes_ok() throws Exception {
        LocalDate from = LocalDate.of(2025, 3, 1);
        LocalDate to   = LocalDate.of(2025, 3, 31);

        ClienteRankingDTO c1 = new ClienteRankingDTO(1L, "Juan", "351...", 5L);
        ClienteRankingDTO c2 = new ClienteRankingDTO(2L, "Ana", "3543...", 3L);

        when(service.rankingClientesFrecuentes(from, to, 10))
                .thenReturn(List.of(c1, c2));

        mvc.perform(get("/api/reportes/clientes-frecuentes")
                        .param("from", "2025-03-01")
                        .param("to", "2025-03-31")
                        .param("top", "10")
                        .with(httpBasic("admin", "admin"))          // 👈 auth
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].clienteNombre").value("Juan"))
                .andExpect(jsonPath("$[0].telefono").value("351..."))
                .andExpect(jsonPath("$[0].cantOrdenes").value(5))
                .andExpect(jsonPath("$[1].clienteNombre").value("Ana"));

        verify(service).rankingClientesFrecuentes(from, to, 10);
    }
}