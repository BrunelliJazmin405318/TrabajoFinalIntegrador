// src/test/java/ar/edu/utn/tfi/web/PublicPagoControllerTest.java
package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.domain.Presupuesto;
import ar.edu.utn.tfi.repository.PresupuestoRepository;
import ar.edu.utn.tfi.service.PresupuestoGestionService;
import ar.edu.utn.tfi.web.dto.PagoApiReq;
import ar.edu.utn.tfi.web.dto.PagoInfoDTO;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicPagoController.class)
@AutoConfigureMockMvc(addFilters = false)   // 👈 DESACTIVA SECURITY EN ESTE TEST
@SuppressWarnings("removal")
class PublicPagoControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    PresupuestoRepository presupuestoRepo;

    @MockBean
    PresupuestoGestionService gestionService;

    @Test
    @DisplayName("GET /public/pagos/api/info-sena/{id} → 200 con DTO si el presupuesto está APROBADO")
    void infoSena_ok() throws Exception {
        Presupuesto p = new Presupuesto();
        p.setId(10L);
        p.setEstado("APROBADO");
        when(presupuestoRepo.findById(10L)).thenReturn(Optional.of(p));

        PagoInfoDTO dto = new PagoInfoDTO(
                10L,
                BigDecimal.valueOf(50000),
                "cliente@test.com",
                "APROBADO",
                "PENDIENTE",
                "in_process",
                "sena-123",
                LocalDateTime.now(),
                null,
                null,
                null,
                null,
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(50000),
                true
        );

        when(gestionService.getPagoInfoPublico(10L)).thenReturn(dto);

        mvc.perform(get("/public/pagos/api/info-sena/{id}", 10L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presupuestoId").value(10L))
                .andExpect(jsonPath("$.clienteEmail").value("cliente@test.com"))
                .andExpect(jsonPath("$.montoSena").value(50000));

        verify(presupuestoRepo).findById(10L);
        verify(gestionService).getPagoInfoPublico(10L);
    }

    @Test
    @DisplayName("GET /public/pagos/api/info-sena/{id} → 409 si el presupuesto no está APROBADO")
    void infoSena_conflict_noAprobado() throws Exception {
        Presupuesto p = new Presupuesto();
        p.setId(20L);
        p.setEstado("PENDIENTE");

        when(presupuestoRepo.findById(20L)).thenReturn(Optional.of(p));

        mvc.perform(get("/public/pagos/api/info-sena/{id}", 20L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("El presupuesto no está APROBADO. Estado actual: PENDIENTE"));

        verify(gestionService, never()).getPagoInfoPublico(anyLong());
    }

    @Test
    @DisplayName("GET /public/pagos/api/info-sena/{id} → 404 si no existe el presupuesto")
    void infoSena_notFound() throws Exception {
        when(presupuestoRepo.findById(99L))
                .thenReturn(Optional.empty());

        mvc.perform(get("/public/pagos/api/info-sena/{id}", 99L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("Presupuesto no encontrado: 99"));

        verify(presupuestoRepo).findById(99L);
        verifyNoInteractions(gestionService);
    }

    @Test
    @DisplayName("POST /public/pagos/api/cobrar-sena/{id} → 200 cuando el cobro se realiza OK")
    void cobrarSena_ok() throws Exception {
        Presupuesto p = new Presupuesto();
        p.setId(30L);
        p.setSenaPaymentStatus("approved");
        p.setSenaPaymentId("pay-123");
        p.setSenaMonto(BigDecimal.valueOf(15000));

        when(gestionService.cobrarSenaApi(eq(30L), any(PagoApiReq.class)))
                .thenReturn(p);

        String body = """
                {
                  "paymentMethodId": "visa",
                  "installments": 1,
                  "issuerId": "123",
                  "payerEmail": "cliente@test.com",
                  "token": "tok-test"
                }
                """;

        mvc.perform(post("/public/pagos/api/cobrar-sena/{id}", 30L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("ok"))
                .andExpect(jsonPath("$.status").value("approved"))
                .andExpect(jsonPath("$.paymentId").value("pay-123"))
                .andExpect(jsonPath("$.monto").value(15000));

        verify(gestionService).cobrarSenaApi(eq(30L), any(PagoApiReq.class));
    }

    @Test
    @DisplayName("POST /public/pagos/api/cobrar-sena/{id} → 400 si el service lanza excepción")
    void cobrarSena_badRequest() throws Exception {
        when(gestionService.cobrarSenaApi(eq(40L), any(PagoApiReq.class)))
                .thenThrow(new RuntimeException("Error de pago"));

        String body = """
                {
                  "paymentMethodId": "visa",
                  "installments": 1,
                  "issuerId": "123",
                  "payerEmail": "cliente@test.com",
                  "token": "tok-test"
                }
                """;

        mvc.perform(post("/public/pagos/api/cobrar-sena/{id}", 40L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error de pago"));

        verify(gestionService).cobrarSenaApi(eq(40L), any(PagoApiReq.class));
    }
}