// src/test/java/ar/edu/utn/tfi/web/AdminFacturaControllerTest.java
package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.domain.FacturaMock;
import ar.edu.utn.tfi.service.FacturaMockService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminFacturaController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminFacturaControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    FacturaMockService facturaService;

    // Helper para armar JSON “a mano” (no necesito ObjectMapper para algo tan simple)
    private String json(String body) {
        return body;
    }

    @Test
    @DisplayName("POST /admin/facturas/generar/{id} → 200 y devuelve FacturaMock")
    void generar_ok() throws Exception {
        FacturaMock f = new FacturaMock();
        f.setId(1L);
        f.setNumero("FB-000001");
        f.setTipo("B");

        when(facturaService.generar(10L, "B")).thenReturn(f);

        mvc.perform(post("/admin/facturas/generar/{presupuestoId}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("{\"tipo\":\"b\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.numero").value("FB-000001"))
                .andExpect(jsonPath("$.tipo").value("B"));

        verify(facturaService).generar(10L, "B");
    }


    @Test
    @DisplayName("POST /admin/facturas/generar/{id} → 404 si el presupuesto no existe")
    void generar_notFound() throws Exception {
        when(facturaService.generar(99L, "B"))
                .thenThrow(new EntityNotFoundException("Presupuesto no encontrado: 99"));

        // Sin body → toma tipo B por defecto
        mvc.perform(post("/admin/facturas/generar/{presupuestoId}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Presupuesto no encontrado: 99"));
    }

    @Test
    @DisplayName("POST /admin/facturas/generar/{id} → 409 si hay conflicto (ya facturado, etc.)")
    void generar_conflict() throws Exception {
        when(facturaService.generar(20L, "B"))
                .thenThrow(new IllegalStateException("Ya existe una factura para este presupuesto."));

        mvc.perform(post("/admin/facturas/generar/{presupuestoId}", 20L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Ya existe una factura para este presupuesto."));
    }

    @Test
    @DisplayName("GET /admin/facturas/{id}/pdf → 200, PDF y header de descarga")
    void pdfByFactura_ok() throws Exception {
        byte[] pdf = "PDF-DUMMY".getBytes();
        when(facturaService.renderPdf(5L)).thenReturn(pdf);

        mvc.perform(get("/admin/facturas/{facturaId}/pdf", 5L))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("factura-5.pdf")))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdf));

        verify(facturaService).renderPdf(5L);
    }

    @Test
    @DisplayName("GET /admin/facturas/{id}/pdf → 404 si no existe la factura")
    void pdfByFactura_notFound() throws Exception {
        when(facturaService.renderPdf(7L))
                .thenThrow(new EntityNotFoundException("Factura no encontrada: 7"));

        mvc.perform(get("/admin/facturas/{facturaId}/pdf", 7L))
                .andExpect(status().isNotFound())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE))
                .andExpect(content().string("Factura no encontrada: 7"));
    }

    @Test
    @DisplayName("GET /admin/facturas/pdf/by-presupuesto/{id} → 200, genera/obtiene factura y devuelve PDF")
    void pdfByPresupuesto_ok() throws Exception {
        byte[] pdf = "PDF-PRESUPUESTO".getBytes();
        when(facturaService.renderPdfByPresupuesto(30L, "A")).thenReturn(pdf);

        mvc.perform(get("/admin/facturas/pdf/by-presupuesto/{id}", 30L)
                        .param("tipo", "a")) // se normaliza a "A"
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("factura-presupuesto-30.pdf")))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdf));

        verify(facturaService).renderPdfByPresupuesto(30L, "A");
    }

    @Test
    @DisplayName("GET /admin/facturas/pdf/by-presupuesto/{id} → 404 si no hay presupuesto/factura")
    void pdfByPresupuesto_notFound() throws Exception {
        when(facturaService.renderPdfByPresupuesto(40L, "B"))
                .thenThrow(new EntityNotFoundException("No hay presupuesto"));

        mvc.perform(get("/admin/facturas/pdf/by-presupuesto/{id}", 40L))
                .andExpect(status().isNotFound())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE))
                .andExpect(content().string("No hay presupuesto"));
    }

    @Test
    @DisplayName("GET /admin/facturas/pdf/by-presupuesto/{id} → 409 si hay conflicto")
    void pdfByPresupuesto_conflict() throws Exception {
        when(facturaService.renderPdfByPresupuesto(50L, "B"))
                .thenThrow(new IllegalStateException("Pago final no acreditado"));

        mvc.perform(get("/admin/facturas/pdf/by-presupuesto/{id}", 50L))
                .andExpect(status().isConflict())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE))
                .andExpect(content().string("Pago final no acreditado"));
    }
}