// src/test/java/ar/edu/utn/tfi/web/PublicPresupuestoControllerTest.java
package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.domain.Presupuesto;
import ar.edu.utn.tfi.domain.SolicitudPresupuesto;
import ar.edu.utn.tfi.service.PresupuestoService;
import ar.edu.utn.tfi.web.dto.SolicitudCreateDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicPresupuestoController.class)
@AutoConfigureMockMvc(addFilters = false)   // 👈 desactiva security para este test
@SuppressWarnings("removal")
class PublicPresupuestoControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    PresupuestoService service;

    @Test
    @DisplayName("POST /public/presupuestos/solicitud → 200 y llama a service.crearSolicitud")
    void crear_ok() throws Exception {
        // Arrange
        SolicitudPresupuesto s = new SolicitudPresupuesto();
        s.setId(10L);
        s.setEstado("PENDIENTE");

        when(service.crearSolicitud(any(SolicitudCreateDTO.class))).thenReturn(s);

        String body = """
                {
                  "clienteNombre": " Juan Perez ",
                  "clienteTelefono": "+54351...",
                  "clienteEmail": "juan@test.com",
                  "tipoUnidad": "motor",
                  "marca": "Ford",
                  "modelo": "Fiesta",
                  "nroMotor": "ABC123",
                  "descripcion": "Algo se rompió",
                  "tipoConsulta": "cotizacion"
                }
                """;

        mvc.perform(post("/public/presupuestos/solicitud")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));

        verify(service).crearSolicitud(any(SolicitudCreateDTO.class));
    }

    @Test
    @DisplayName("POST /public/presupuestos/solicitud → 400 si hay errores de validación manual")
    void crear_validationError() throws Exception {
        String body = """
                {
                  "clienteNombre": "",
                  "clienteTelefono": "",
                  "clienteEmail": "mal-formato",
                  "tipoUnidad": "",
                  "marca": "",
                  "modelo": "",
                  "nroMotor": "",
                  "descripcion": "",
                  "tipoConsulta": ""
                }
                """;

        mvc.perform(post("/public/presupuestos/solicitud")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.clienteNombre").exists())
                .andExpect(jsonPath("$.errors.clienteTelefono").exists())
                .andExpect(jsonPath("$.errors.clienteEmail").exists())
                .andExpect(jsonPath("$.errors.tipoUnidad").exists())
                .andExpect(jsonPath("$.errors.descripcion").exists());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("GET /public/presupuestos/solicitud/{id} → devuelve solicitud + presupuesto aprobado si existe")
    void ver_ok_con_presupuesto_aprobado() throws Exception {
        // Solicitud
        SolicitudPresupuesto s = new SolicitudPresupuesto();
        s.setId(5L);
        s.setClienteNombre("Carlos");
        s.setClienteTelefono("351...");
        s.setClienteEmail("carlos@test.com");
        s.setTipoUnidad("MOTOR");
        s.setMarca("Ford");
        s.setModelo("Fiesta");
        s.setNroMotor("XYZ123");
        s.setDescripcion("Golpe en cilindro");
        s.setTipoConsulta("COTIZACION");
        s.setEstado("APROBADA");

        // Presupuesto aprobado asociado
        Presupuesto p = new Presupuesto();
        p.setId(99L);
        p.setEstado("APROBADO");
        p.setTotal(BigDecimal.valueOf(123456));
        p.setSenaEstado("PENDIENTE");
        p.setSenaMonto(BigDecimal.valueOf(20000));
        p.setSenaPaymentId("pay-123");
        p.setSenaPaymentStatus("pending");
        p.setOtNroOrden("OT-0001");

        when(service.getById(5L)).thenReturn(s);
        when(service.listar("APROBADO", 5L)).thenReturn(List.of(p));

        mvc.perform(get("/public/presupuestos/solicitud/{id}", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.clienteNombre").value("Carlos"))
                .andExpect(jsonPath("$.clienteTelefono").value("351..."))
                .andExpect(jsonPath("$.clienteEmail").value("carlos@test.com"))
                // datos del presupuesto
                .andExpect(jsonPath("$.presupuestoId").value(99L))
                .andExpect(jsonPath("$.presupuestoEstado").value("APROBADO"))
                .andExpect(jsonPath("$.total").value(123456))
                .andExpect(jsonPath("$.senaEstado").value("PENDIENTE"))
                .andExpect(jsonPath("$.senaMonto").value(20000))
                .andExpect(jsonPath("$.otNroOrden").value("OT-0001"));

        verify(service).getById(5L);
        verify(service).listar("APROBADO", 5L);
    }
}