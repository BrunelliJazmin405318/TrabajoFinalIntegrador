// src/test/java/ar/edu/utn/tfi/web/ReportesExportControllerTest.java
package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.security.SecurityConfig;
import ar.edu.utn.tfi.service.ReporteExportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportesExportController.class)
@Import(SecurityConfig.class)
@SuppressWarnings("removal")
class ReportesExportControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    ReporteExportService exportService;

    @Test
    @DisplayName("GET /api/reportes/export clientes xlsx → devuelve archivo Excel y llama a clientesFrecuentesXlsx")
    void export_clientes_xlsx_ok() throws Exception {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to   = LocalDate.of(2025, 1, 31);

        byte[] bytes = "XLSX-CLIENTES".getBytes();
        when(exportService.clientesFrecuentesXlsx(from, to, 10))
                .thenReturn(bytes);

        mvc.perform(get("/api/reportes/export")
                        .param("reporte", "clientes")
                        .param("from", "2025-01-01")
                        .param("to", "2025-01-31")
                        .param("format", "xlsx")
                        .param("top", "10")
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                // content-type Excel
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                // filename en Content-Disposition
                .andExpect(header().string("Content-Disposition",
                        containsString("clientes-2025-01-01_2025-01-31.xlsx")))
                .andExpect(content().bytes(bytes));

        verify(exportService).clientesFrecuentesXlsx(from, to, 10);
    }

    @Test
    @DisplayName("GET /api/reportes/export motores-vs-tapas pdf → devuelve PDF y llama a motoresVsTapasPdf")
    void export_motoresVsTapas_pdf_ok() throws Exception {
        LocalDate from = LocalDate.of(2025, 2, 1);
        LocalDate to   = LocalDate.of(2025, 2, 28);

        byte[] bytes = "PDF-MOTORES-VS-TAPAS".getBytes();
        when(exportService.motoresVsTapasPdf(from, to))
                .thenReturn(bytes);

        mvc.perform(get("/api/reportes/export")
                        .param("reporte", "motores-vs-tapas")
                        .param("from", "2025-02-01")
                        .param("to", "2025-02-28")
                        .param("format", "pdf")
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string("Content-Disposition",
                        containsString("motores-vs-tapas-2025-02-01_2025-02-28.pdf")))
                .andExpect(content().bytes(bytes));

        verify(exportService).motoresVsTapasPdf(from, to);
    }
}