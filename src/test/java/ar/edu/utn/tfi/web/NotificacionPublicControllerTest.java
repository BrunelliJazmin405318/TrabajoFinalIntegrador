// src/test/java/ar/edu/utn/tfi/web/NotificacionPublicControllerTest.java
package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.domain.Notificacion;
import ar.edu.utn.tfi.repository.NotificacionRepository;
import ar.edu.utn.tfi.service.NotificationHub;
import ar.edu.utn.tfi.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificacionPublicController.class)
@Import(SecurityConfig.class)
@SuppressWarnings("removal")
class NotificacionPublicControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    NotificationHub hub;

    @MockBean
    NotificacionRepository repo;

    @Test
    @DisplayName("GET /public/notificaciones/ultimas → devuelve lista de notificaciones")
    void ultimas_ok() throws Exception {
        Notificacion n1 = new Notificacion();
        n1.setId(1L);
        n1.setNroOrden("OT-0001");
        n1.setMessage("Mensaje 1");
        n1.setEstado("NUEVA");
        n1.setCreatedAt(LocalDateTime.now());

        Notificacion n2 = new Notificacion();
        n2.setId(2L);
        n2.setNroOrden("OT-0001");
        n2.setMessage("Mensaje 2");
        n2.setEstado("NUEVA");
        n2.setCreatedAt(LocalDateTime.now());

        when(repo.findTop20ByNroOrdenOrderByCreatedAtDesc("OT-0001"))
                .thenReturn(List.of(n1, n2));

        mvc.perform(get("/public/notificaciones/ultimas")
                        .param("nroOrden", "OT-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(repo).findTop20ByNroOrdenOrderByCreatedAtDesc("OT-0001");
    }

    @Test
    @DisplayName("POST /public/notificaciones/{id}/leida → marca como LEIDA y guarda")
    void marcarLeida_ok() throws Exception {
        Notificacion n = new Notificacion();
        n.setId(5L);
        n.setEstado("NUEVA");

        when(repo.findById(5L)).thenReturn(Optional.of(n));

        mvc.perform(post("/public/notificaciones/{id}/leida", 5L))
                .andExpect(status().isOk());

        // Se cambia el estado y se guarda
        verify(repo).findById(5L);
        verify(repo).save(n);
    }
}