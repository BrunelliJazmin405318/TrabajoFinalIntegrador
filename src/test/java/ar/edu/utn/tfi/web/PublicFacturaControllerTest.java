// src/test/java/ar/edu/utn/tfi/web/PublicFacturaControllerTest.java
package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.security.SecurityConfig;
import ar.edu.utn.tfi.service.FacturaMockService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicFacturaController.class)
@Import(SecurityConfig.class)
@SuppressWarnings("removal")
class PublicFacturaControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    FacturaMockService facturaService;

    @Test
    @DisplayName("GET /public/facturas/pdf/by-solicitud/{id} → 200 y PDF con header")
    void pdfBySolicitud_ok() throws Exception {
        byte[] pdf = "PDF-SOLICITUD".getBytes();
        when(facturaService.renderPdfBySolicitud(10L)).thenReturn(pdf);

        mvc.perform(get("/public/facturas/pdf/by-solicitud/{id}", 10L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("factura-solicitud-10.pdf")))
                .andExpect(content().bytes(pdf));

        verify(facturaService).renderPdfBySolicitud(10L);
    }

    @Test
    @DisplayName("GET /public/facturas/pdf/by-solicitud/{id} → 404 NOT_FOUND")
    void pdfBySolicitud_notFound() throws Exception {
        when(facturaService.renderPdfBySolicitud(20L))
                .thenThrow(new EntityNotFoundException("Factura no encontrada"));

        mvc.perform(get("/public/facturas/pdf/by-solicitud/{id}", 20L))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("\"error\":\"NOT_FOUND\"")))
                .andExpect(content().string(containsString("Factura no encontrada")));

        verify(facturaService).renderPdfBySolicitud(20L);
    }

    @Test
    @DisplayName("GET /public/facturas/pdf/by-solicitud/{id} → 409 CONFLICT")
    void pdfBySolicitud_conflict() throws Exception {
        when(facturaService.renderPdfBySolicitud(30L))
                .thenThrow(new IllegalStateException("Pago final no acreditado"));

        mvc.perform(get("/public/facturas/pdf/by-solicitud/{id}", 30L))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("\"error\":\"CONFLICT\"")))
                .andExpect(content().string(containsString("Pago final no acreditado")));

        verify(facturaService).renderPdfBySolicitud(30L);
    }

    @Test
    @DisplayName("GET /public/facturas/pdf/by-solicitud/{id} → 500 INTERNAL_ERROR")
    void pdfBySolicitud_internalError() throws Exception {
        when(facturaService.renderPdfBySolicitud(40L))
                .thenThrow(new RuntimeException("Algo explotó"));

        mvc.perform(get("/public/facturas/pdf/by-solicitud/{id}", 40L))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("\"error\":\"INTERNAL_ERROR\"")))
                // Para que no moleste el encoding, sólo verificamos que aparece "Algo"
                .andExpect(content().string(containsString("Algo")));

        verify(facturaService).renderPdfBySolicitud(40L);
    }
}