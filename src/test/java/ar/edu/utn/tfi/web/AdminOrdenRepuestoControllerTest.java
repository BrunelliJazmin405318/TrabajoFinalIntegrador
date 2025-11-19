// src/test/java/ar/edu/utn/tfi/web/AdminOrdenRepuestoControllerTest.java
package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.domain.OrdenRepuesto;
import ar.edu.utn.tfi.domain.OrdenTrabajo;
import ar.edu.utn.tfi.repository.OrdenRepuestoRepository;
import ar.edu.utn.tfi.repository.OrdenTrabajoRepository;
import ar.edu.utn.tfi.security.SecurityConfig;
import ar.edu.utn.tfi.service.OrdenRepuestoService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic; // 👈 ESTE es el import correcto
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminOrdenRepuestoController.class)
@Import(SecurityConfig.class)
@SuppressWarnings("removal") // 👈 para el warning de @MockBean
class AdminOrdenRepuestoControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    OrdenRepuestoService repuestoService;

    @MockBean
    OrdenRepuestoRepository ordenRepuestoRepository;

    @MockBean
    OrdenTrabajoRepository ordenRepo;

    @Test
    @DisplayName("GET /admin/ordenes/{nro}/repuestos → lista vacía si no hay repuestos")
    void listarRepuestos_ok() throws Exception {
        when(repuestoService.listarPorNro("OT-0001"))
                .thenReturn(Collections.emptyList());

        mvc.perform(get("/admin/ordenes/{nro}/repuestos", "OT-0001")
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(repuestoService).listarPorNro("OT-0001");
    }

    @Test
    @DisplayName("POST /admin/ordenes/{nro}/repuestos → llama a repuestoService.agregar con usuario autenticado")
    void agregarRepuesto_ok() throws Exception {
        String bodyJson = """
                {
                  "descripcion": "Filtro de aceite",
                  "cantidad": 2,
                  "precioUnit": 1500.0
                }
                """;

        mvc.perform(post("/admin/ordenes/{nro}/repuestos", "OT-0002")
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson))
                .andExpect(status().isOk());

        verify(repuestoService).agregar(eq("OT-0002"), any(), eq("admin"));
    }

    @Test
    @DisplayName("DELETE /admin/ordenes/{nro}/repuestos/{id} → borra repuesto y devuelve mensaje OK")
    void eliminarRepuesto_ok() throws Exception {
        mvc.perform(delete("/admin/ordenes/{nro}/repuestos/{id}", "OT-0003", 10L)
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Repuesto eliminado"))
                .andExpect(jsonPath("$.nroOrden").value("OT-0003"))
                .andExpect(jsonPath("$.repuestoId").value(10L));

        verify(repuestoService).eliminar("OT-0003", 10L, "admin");
    }

    @Test
    @DisplayName("PUT /admin/ordenes/{nro}/repuestos/{id} → actualiza cantidad y precio")
    void actualizarRepuesto_ok() throws Exception {
        OrdenTrabajo ot = new OrdenTrabajo();
        ot.setId(1L);
        ot.setNroOrden("OT-0004");
        when(ordenRepo.findByNroOrden("OT-0004")).thenReturn(Optional.of(ot));

        OrdenRepuesto rep = new OrdenRepuesto();
        rep.setId(20L);
        rep.setOrdenId(1L);
        rep.setCantidad(new BigDecimal("1"));
        rep.setPrecioUnit(new BigDecimal("1000"));

        when(ordenRepuestoRepository.findById(20L)).thenReturn(Optional.of(rep));
        when(ordenRepuestoRepository.save(any(OrdenRepuesto.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        String bodyJson = """
                {
                  "cantidad": 3,
                  "precioUnit": 2000.0
                }
                """;

        mvc.perform(put("/admin/ordenes/{nro}/repuestos/{repId}", "OT-0004", 20L)
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson))
                .andExpect(status().isOk());

        verify(ordenRepo).findByNroOrden("OT-0004");
        verify(ordenRepuestoRepository).findById(20L);
        verify(ordenRepuestoRepository).save(any(OrdenRepuesto.class));
    }

    @Test
    @DisplayName("PUT /admin/ordenes/{nro}/repuestos/{id} → 400 si el repuesto no pertenece a la orden")
    void actualizarRepuesto_repuestoDeOtraOrden() throws Exception {
        OrdenTrabajo ot = new OrdenTrabajo();
        ot.setId(1L);
        ot.setNroOrden("OT-0005");
        when(ordenRepo.findByNroOrden("OT-0005")).thenReturn(Optional.of(ot));

        OrdenRepuesto rep = new OrdenRepuesto();
        rep.setId(30L);
        rep.setOrdenId(99L);
        when(ordenRepuestoRepository.findById(30L)).thenReturn(Optional.of(rep));

        String bodyJson = """
                {
                  "cantidad": 1,
                  "precioUnit": 1000.0
                }
                """;

        mvc.perform(put("/admin/ordenes/{nro}/repuestos/{repId}", "OT-0005", 30L)
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson))
                .andExpect(status().isBadRequest());

        verify(ordenRepo).findByNroOrden("OT-0005");
        verify(ordenRepuestoRepository).findById(30L);
        verify(ordenRepuestoRepository, never()).save(any());
    }

    @Test
    @DisplayName("GET /admin/ordenes/{nro}/repuestos/total → devuelve el número de orden y el total")
    void totalRepuestos_ok() throws Exception {
        when(repuestoService.calcularTotalRepuestosPorNroOrden("OT-0006"))
                .thenReturn(new BigDecimal("12345.67"));

        mvc.perform(get("/admin/ordenes/{nro}/repuestos/total", "OT-0006")
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nroOrden").value("OT-0006"))
                .andExpect(jsonPath("$.totalRepuestos").value(12345.67));

        verify(repuestoService).calcularTotalRepuestosPorNroOrden("OT-0006");
    }
}