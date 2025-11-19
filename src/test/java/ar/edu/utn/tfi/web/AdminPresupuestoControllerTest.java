// src/test/java/ar/edu/utn/tfi/web/AdminPresupuestoControllerTest.java
package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.domain.FacturaMock;
import ar.edu.utn.tfi.domain.Presupuesto;
import ar.edu.utn.tfi.domain.SolicitudPresupuesto;
import ar.edu.utn.tfi.repository.FacturaMockRepository;
import ar.edu.utn.tfi.repository.PresupuestoRepository;
import ar.edu.utn.tfi.security.SecurityConfig;
import ar.edu.utn.tfi.service.PresupuestoGestionService;
import ar.edu.utn.tfi.service.PresupuestoService;
import ar.edu.utn.tfi.web.dto.PresupuestoAdminDTO;
import ar.edu.utn.tfi.web.dto.SolicitudDTO;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminPresupuestoController.class)
@Import(SecurityConfig.class)
@SuppressWarnings("removal")
class AdminPresupuestoControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    PresupuestoService solicitudesService;          // SOLICITUDES

    @MockBean
    PresupuestoGestionService presupuestosService;  // PRESUPUESTOS

    @MockBean
    FacturaMockRepository facturaRepo;

    @MockBean
    PresupuestoRepository presupuestoRepo;

    // ---------- SOLICITUDES ----------

    @Test
    @DisplayName("GET /admin/presupuestos/solicitudes → lista solicitudes, usando flag generado")
    void listarSolicitudes_ok() throws Exception {
        SolicitudPresupuesto s = new SolicitudPresupuesto();
        s.setId(1L);

        when(solicitudesService.listar(null)).thenReturn(List.of(s));
        when(presupuestoRepo.existsBySolicitudId(1L)).thenReturn(true);

        mvc.perform(get("/admin/presupuestos/solicitudes")
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                // Sólo verificamos que venga un array de tamaño 1
                .andExpect(jsonPath("$.length()").value(1));

        verify(solicitudesService).listar(null);
        verify(presupuestoRepo).existsBySolicitudId(1L);
    }

    @Test
    @DisplayName("PUT /admin/presupuestos/solicitudes/{id}/aprobar → OK")
    void aprobar_ok() throws Exception {
        SolicitudPresupuesto s = new SolicitudPresupuesto();
        s.setId(10L);

        when(solicitudesService.aprobar(eq(10L), eq("admin"), eq("todo ok")))
                .thenReturn(s);

        String bodyJson = """
                { "nota": "todo ok" }
                """;

        mvc.perform(put("/admin/presupuestos/solicitudes/{id}/aprobar", 10L)
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Aprobada"))
                .andExpect(jsonPath("$.id").value(10L));

        verify(solicitudesService).aprobar(10L, "admin", "todo ok");
    }

    @Test
    @DisplayName("PUT /admin/presupuestos/solicitudes/{id}/aprobar → 404 si no existe")
    void aprobar_notFound() throws Exception {
        when(solicitudesService.aprobar(eq(99L), anyString(), any()))
                .thenThrow(new EntityNotFoundException("Solicitud no encontrada"));

        mvc.perform(put("/admin/presupuestos/solicitudes/{id}/aprobar", 99L)
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nota\":\"x\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Solicitud no encontrada"));
    }

    @Test
    @DisplayName("PUT /admin/presupuestos/solicitudes/{id}/aprobar → 409 si hay conflicto")
    void aprobar_conflict() throws Exception {
        when(solicitudesService.aprobar(eq(50L), anyString(), any()))
                .thenThrow(new IllegalStateException("Ya está aprobada"));

        mvc.perform(put("/admin/presupuestos/solicitudes/{id}/aprobar", 50L)
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nota\":\"x\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Ya está aprobada"));
    }

    @Test
    @DisplayName("PUT /admin/presupuestos/solicitudes/{id}/rechazar → OK")
    void rechazar_ok() throws Exception {
        SolicitudPresupuesto s = new SolicitudPresupuesto();
        s.setId(20L);

        when(solicitudesService.rechazar(eq(20L), eq("admin"), eq("mal estado")))
                .thenReturn(s);

        String bodyJson = """
                { "nota": "mal estado" }
                """;

        mvc.perform(put("/admin/presupuestos/solicitudes/{id}/rechazar", 20L)
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Rechazada"))
                .andExpect(jsonPath("$.id").value(20L));

        verify(solicitudesService).rechazar(20L, "admin", "mal estado");
    }

    // ---------- PRESUPUESTOS (grilla) ----------

    @Test
    @DisplayName("GET /admin/presupuestos → devuelve lista vacía si no hay datos")
    void listar_vacio() throws Exception {
        when(presupuestosService.listar(null, null)).thenReturn(List.of());

        mvc.perform(get("/admin/presupuestos")
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(presupuestosService).listar(null, null);
        verifyNoInteractions(facturaRepo);
    }

    @Test
    @DisplayName("GET /admin/presupuestos → devuelve lista de PresupuestoAdminDTO")
    void listar_ok() throws Exception {
        // Presupuesto base
        Presupuesto p = new Presupuesto();
        p.setId(1L);

        // Simulamos lo que devolvería el service (presupuestos)
        when(presupuestosService.listar("APROBADO", null)).thenReturn(List.of(p));

        // Factura asociada a ese presupuesto
        FacturaMock f = new FacturaMock();
        f.setNumero("FA-0001");
        f.setTipo("A");
        f.setPresupuesto(p);

        when(facturaRepo.findByPresupuestoIdIn(List.of(1L)))
                .thenReturn(List.of(f));

        mvc.perform(get("/admin/presupuestos")
                        .param("estado", "APROBADO")
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(presupuestosService).listar("APROBADO", null);
        verify(facturaRepo).findByPresupuestoIdIn(List.of(1L));
    }

    @Test
    @DisplayName("GET /admin/presupuestos/solicitudes/{id} → devuelve SolicitudDTO con flag generado")
    void getSolicitudById_ok() throws Exception {
        SolicitudPresupuesto s = new SolicitudPresupuesto();
        s.setId(77L);

        when(solicitudesService.getById(77L)).thenReturn(s);
        when(presupuestoRepo.existsBySolicitudId(77L)).thenReturn(true);

        mvc.perform(get("/admin/presupuestos/solicitudes/{id}", 77L)
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk());

        verify(solicitudesService).getById(77L);
        verify(presupuestoRepo).existsBySolicitudId(77L);
    }
}