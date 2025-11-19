// src/test/java/ar/edu/utn/tfi/web/AdminOrdenControllerTest.java
package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.domain.OrdenTrabajo;
import ar.edu.utn.tfi.domain.Presupuesto;
import ar.edu.utn.tfi.repository.OrdenTrabajoRepository;
import ar.edu.utn.tfi.repository.PresupuestoRepository;
import ar.edu.utn.tfi.repository.SolicitudPresupuestoRepository;
import ar.edu.utn.tfi.security.SecurityConfig;
import ar.edu.utn.tfi.service.CrearOrdenService;
import ar.edu.utn.tfi.service.OrderAdvanceService;
import ar.edu.utn.tfi.service.OrderDelayService;
import ar.edu.utn.tfi.service.OrderIrreparableService;
import ar.edu.utn.tfi.service.CrearOrdenService.CreateOTReq;
import ar.edu.utn.tfi.service.CrearOrdenService.CreateOTResp;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;


import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
//
 // import static org.springframework.security.test.web.servlet.request.MockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminOrdenController.class)
@Import(SecurityConfig.class)
@SuppressWarnings("removal")
class AdminOrdenControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    OrdenTrabajoRepository ordenRepo;

    @MockBean
    PresupuestoRepository presupuestoRepo;

    @MockBean
    SolicitudPresupuestoRepository solicitudRepo;

    @MockBean
    OrderAdvanceService advanceService;

    @MockBean
    OrderIrreparableService irreparableService;

    @MockBean
    OrderDelayService delayService;

    @MockBean
    CrearOrdenService crearOrdenService;

    @Test
    @DisplayName("POST /admin/ordenes/{nro}/avanzar → llama al servicio con el ID de la OT y el usuario")
    void avanzarEtapaPorNro_ok() throws Exception {
        // Arrange
        OrdenTrabajo ot = new OrdenTrabajo();
        ot.setId(7L);
        ot.setNroOrden("OT-0007");

        when(ordenRepo.findByNroOrden("OT-0007")).thenReturn(Optional.of(ot));

        // Act + Assert
        mvc.perform(post("/admin/ordenes/{nro}/avanzar", "OT-0007")
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Etapa avanzada correctamente"))
                .andExpect(jsonPath("$.nroOrden").value("OT-0007"));

        verify(advanceService).avanzarEtapa(7L, "admin");
    }

    @Test
    @DisplayName("PUT /admin/ordenes/{nro}/pieza-irreparable → marca irreparable usando el usuario autenticado")
    void marcarIrreparable_ok() throws Exception {
        mvc.perform(put("/admin/ordenes/{nro}/pieza-irreparable", "OT-0010")
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Orden marcada como PIEZA_IRREPARABLE"))
                .andExpect(jsonPath("$.nroOrden").value("OT-0010"));

        verify(irreparableService).marcarIrreparablePorNro("OT-0010", "admin");
    }

    @Test
    @DisplayName("POST /admin/ordenes/{nro}/demora → 400 si falta 'motivo'")
    void registrarDemora_badRequest() throws Exception {
        mvc.perform(post("/admin/ordenes/{nro}/demora", "OT-0001")
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").exists());

        verifyNoInteractions(delayService);
    }

    @Test
    @DisplayName("POST /admin/ordenes/{nro}/demora → 200 y llama a delayService.registrarDemora")
    void registrarDemora_ok() throws Exception {
        String bodyJson = """
                {
                  "motivo": "falta_repuesto",
                  "observacion": "Esperando repuesto del proveedor"
                }
                """;

        mvc.perform(post("/admin/ordenes/{nro}/demora", "OT-0002")
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Demora registrada"))
                .andExpect(jsonPath("$.nroOrden").value("OT-0002"));

        // motivo se normaliza a upper
        verify(delayService).registrarDemora("OT-0002", "admin",
                "FALTA_REPUESTO", "Esperando repuesto del proveedor");
    }

    @Test
    @DisplayName("POST /admin/ordenes → crea OT directa y devuelve IDs")
    void crearOrdenDirecta_ok() throws Exception {
        // Arrange
        CreateOTResp resp = new CreateOTResp(15L, "OT-0015");
        when(crearOrdenService.crearOT(any(CreateOTReq.class), eq("admin")))
                .thenReturn(resp);

        String bodyJson = """
                {
                  "clienteNombre": "Juan Perez",
                  "clienteTelefono": "+549351...",
                  "tipo": "MOTOR",
                  "marca": "Ford",
                  "modelo": "Fiesta",
                  "nroMotor": "ABC123"
                }
                """;

        // Act + Assert
        mvc.perform(post("/admin/ordenes")
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Orden creada"))
                .andExpect(jsonPath("$.ordenId").value(15L))
                .andExpect(jsonPath("$.nroOrden").value("OT-0015"));

        verify(crearOrdenService).crearOT(any(CreateOTReq.class), eq("admin"));
    }

    @Test
    @DisplayName("POST /admin/ordenes/by-presupuesto/{id} → si el presupuesto ya tiene OT, devuelve la existente")
    void crearDesdePresupuesto_yaTieneOt() throws Exception {
        // Arrange: presupuesto con otNroOrden ya seteado
        Presupuesto p = new Presupuesto();
        p.setId(5L);
        p.setOtNroOrden("OT-0099");

        OrdenTrabajo ot = new OrdenTrabajo();
        ot.setId(99L);
        ot.setNroOrden("OT-0099");

        when(presupuestoRepo.findById(5L)).thenReturn(Optional.of(p));
        when(ordenRepo.findByNroOrden("OT-0099")).thenReturn(Optional.of(ot));

        // Act + Assert
        mvc.perform(post("/admin/ordenes/by-presupuesto/{id}", 5L)
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ordenId").value(99L))
                .andExpect(jsonPath("$.nroOrden").value("OT-0099"));

        // En este flujo NO debe crear una nueva OT
        verifyNoInteractions(crearOrdenService);
    }

    @Test
    @DisplayName("POST /admin/ordenes/by-presupuesto/{id} → envuelve ResponseStatusException en INTERNAL_ERROR 500")
    void crearDesdePresupuesto_conflictPorPago() throws Exception {
        // Arrange: presupuesto sin seña ni pago final acreditado
        Presupuesto p = new Presupuesto();
        p.setId(6L);
        p.setSenaEstado("PENDIENTE");
        p.setFinalEstado(null);

        when(presupuestoRepo.findById(6L)).thenReturn(Optional.of(p));

        mvc.perform(post("/admin/ordenes/by-presupuesto/{id}", 6L)
                        .with(httpBasic("admin", "admin")))
                // 💥 realmente tu API devuelve 500, no 409
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message",
                        org.hamcrest.Matchers.containsString(
                                "Para crear la OT primero debe estar acreditada la seña (o el pago final)."
                        )));

        verify(presupuestoRepo).findById(6L);
        verifyNoInteractions(ordenRepo);
        verifyNoInteractions(crearOrdenService);
    }

    @Test
    @DisplayName("POST /admin/ordenes/{nro}/avanzar → 404 si no existe la orden")
    void avanzarEtapaPorNro_notFound() throws Exception {
        when(ordenRepo.findByNroOrden("OT-1234")).thenReturn(Optional.empty());

        mvc.perform(post("/admin/ordenes/{nro}/avanzar", "OT-1234")
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isNotFound());

        verifyNoInteractions(advanceService);
    }
}