// src/test/java/ar/edu/utn/tfi/web/OrdenPublicControllerTest.java
package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.security.SecurityConfig;
import ar.edu.utn.tfi.service.AuditoriaService;
import ar.edu.utn.tfi.service.OrderQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrdenPublicController.class)
@Import(SecurityConfig.class)
@SuppressWarnings("removal")
class OrdenPublicControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    OrderQueryService orderQueryService;

    @MockBean
    AuditoriaService auditoriaService;

    @Test
    @DisplayName("GET /public/ordenes/{nro}/estado → llama al servicio y devuelve 200")
    void estado_ok() throws Exception {
        when(orderQueryService.getPublicDetailsByNro("OT-0001"))
                .thenReturn(null); // no nos importa el contenido para este test

        mvc.perform(get("/public/ordenes/{nroOrden}/estado", "OT-0001"))
                .andExpect(status().isOk());

        verify(orderQueryService).getPublicDetailsByNro("OT-0001");
    }

    @Test
    @DisplayName("GET /public/ordenes/{nro}/historial → lista vacía si no hay etapas")
    void historial_ok() throws Exception {
        when(orderQueryService.getHistorialByNro("OT-0002"))
                .thenReturn(Collections.emptyList());

        mvc.perform(get("/public/ordenes/{nroOrden}/historial", "OT-0002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(orderQueryService).getHistorialByNro("OT-0002");
    }

    @Test
    @DisplayName("GET /public/ordenes/{nro}/auditoria → lista vacía si no hay cambios")
    void auditoria_ok() throws Exception {
        when(auditoriaService.listarPorNro("OT-0003"))
                .thenReturn(Collections.emptyList());

        mvc.perform(get("/public/ordenes/{nroOrden}/auditoria", "OT-0003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(auditoriaService).listarPorNro("OT-0003");
    }
}