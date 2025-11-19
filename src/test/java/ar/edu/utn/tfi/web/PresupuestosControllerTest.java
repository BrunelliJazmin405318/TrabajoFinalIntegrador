// src/test/java/ar/edu/utn/tfi/web/PresupuestosControllerTest.java
package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.domain.Presupuesto;
import ar.edu.utn.tfi.domain.PresupuestoItem;
import ar.edu.utn.tfi.repository.PresupuestoItemRepository;
import ar.edu.utn.tfi.security.SecurityConfig;
import ar.edu.utn.tfi.service.PresupuestoGestionService;
import ar.edu.utn.tfi.web.dto.PresupuestoDTO;
import ar.edu.utn.tfi.web.dto.PresupuestoGenerarReq;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PresupuestosController.class)
@Import(SecurityConfig.class)
@SuppressWarnings("removal")
class PresupuestosControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    PresupuestoGestionService service;

    @MockBean
    PresupuestoItemRepository itemRepo;

    @Test
    @DisplayName("POST /presupuestos/generar → crea presupuesto y devuelve 200")
    void generar_ok() throws Exception {
        Presupuesto p = new Presupuesto();
        p.setId(1L);

        when(service.generarDesdeSolicitud(
                eq(10L),
                eq("AUTO"),
                eq("MOTOR"),
                anyList(),
                anyList()
        )).thenReturn(p);

        when(itemRepo.findByPresupuestoId(1L))
                .thenReturn(List.of());

        String bodyJson = """
                {
                  "solicitudId": 10,
                  "vehiculoTipo": "AUTO",
                  "piezaTipo": "MOTOR",
                  "servicios": [],
                  "extras": []
                }
                """;

        mvc.perform(post("/presupuestos/generar")
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson))
                .andExpect(status().isOk());

        verify(service).generarDesdeSolicitud(eq(10L), eq("AUTO"), eq("MOTOR"),
                anyList(), anyList());
        verify(itemRepo).findByPresupuestoId(1L);
    }

    @Test
    @DisplayName("GET /admin/presupuestos/{id} → 200 y consulta servicio + items")
    void ver_ok() throws Exception {
        Presupuesto p = new Presupuesto();
        p.setId(5L);

        when(service.getById(5L)).thenReturn(p);
        when(itemRepo.findByPresupuestoId(5L)).thenReturn(List.of());

        mvc.perform(get("/admin/presupuestos/{id}", 5L)
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk());

        verify(service).getById(5L);
        verify(itemRepo).findByPresupuestoId(5L);
    }

    @Test
    @DisplayName("PUT /admin/presupuestos/{id}/aprobar → OK")
    void aprobar_ok() throws Exception {
        Presupuesto p = new Presupuesto();
        p.setId(7L);

        when(service.aprobar(eq(7L), eq("admin"), eq("todo ok")))
                .thenReturn(p);

        String bodyJson = """
                { "nota": "todo ok" }
                """;

        mvc.perform(put("/admin/presupuestos/{id}/aprobar", 7L)
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Aprobado"))
                .andExpect(jsonPath("$.id").value(7L));

        verify(service).aprobar(7L, "admin", "todo ok");
    }

    @Test
    @DisplayName("PUT /admin/presupuestos/{id}/aprobar → 409 si IllegalState")
    void aprobar_conflict() throws Exception {
        when(service.aprobar(eq(8L), eq("admin"), isNull()))
                .thenThrow(new IllegalStateException("No se puede aprobar en estado X"));

        mvc.perform(put("/admin/presupuestos/{id}/aprobar", 8L)
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("No se puede aprobar en estado X"));

        verify(service).aprobar(8L, "admin", null);
    }

    @Test
    @DisplayName("PUT /admin/presupuestos/{id}/aprobar → 404 si no existe")
    void aprobar_notFound() throws Exception {
        when(service.aprobar(eq(9L), eq("admin"), isNull()))
                .thenThrow(new EntityNotFoundException("Presupuesto no encontrado"));

        mvc.perform(put("/admin/presupuestos/{id}/aprobar", 9L)
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Presupuesto no encontrado"));

        verify(service).aprobar(9L, "admin", null);
    }

    @Test
    @DisplayName("PUT /admin/presupuestos/{id}/rechazar → OK")
    void rechazar_ok() throws Exception {
        Presupuesto p = new Presupuesto();
        p.setId(11L);

        when(service.rechazar(eq(11L), eq("admin"), eq("muy caro")))
                .thenReturn(p);

        String bodyJson = """
                { "nota": "muy caro" }
                """;

        mvc.perform(put("/admin/presupuestos/{id}/rechazar", 11L)
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Rechazado"))
                .andExpect(jsonPath("$.id").value(11L));

        verify(service).rechazar(11L, "admin", "muy caro");
    }

    @Test
    @DisplayName("PUT /admin/presupuestos/{id}/rechazar → 409 si IllegalState")
    void rechazar_conflict() throws Exception {
        when(service.rechazar(eq(12L), eq("admin"), isNull()))
                .thenThrow(new IllegalStateException("No se puede rechazar en estado Y"));

        mvc.perform(put("/admin/presupuestos/{id}/rechazar", 12L)
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("No se puede rechazar en estado Y"));

        verify(service).rechazar(12L, "admin", null);
    }

    @Test
    @DisplayName("PUT /admin/presupuestos/{id}/rechazar → 404 si no existe")
    void rechazar_notFound() throws Exception {
        when(service.rechazar(eq(13L), eq("admin"), isNull()))
                .thenThrow(new EntityNotFoundException("Presupuesto no encontrado"));

        mvc.perform(put("/admin/presupuestos/{id}/rechazar", 13L)
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Presupuesto no encontrado"));

        verify(service).rechazar(13L, "admin", null);
    }
}