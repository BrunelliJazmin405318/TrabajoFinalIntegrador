// src/test/java/ar/edu/utn/tfi/web/NotificationControllerTest.java
package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.domain.Notificacion;
import ar.edu.utn.tfi.security.SecurityConfig;
import ar.edu.utn.tfi.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@Import(SecurityConfig.class)
@SuppressWarnings("removal")
class NotificationControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    NotificationService service;

    @Test
    @DisplayName("GET /notifications/unread → usa email normalizado y devuelve lista")
    void unread_ok() throws Exception {
        Notificacion n = new Notificacion();
        n.setId(1L);

        when(service.unread("user@mail.com")).thenReturn(List.of(n));

        mvc.perform(get("/notifications/unread")
                        .param("email", "  User@Mail.com  ")
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // El controller trim + lower-case
        verify(service).unread("user@mail.com");
    }

    @Test
    @DisplayName("POST /notifications/{id}/read → marca leída y devuelve ok=true")
    void read_ok() throws Exception {
        mvc.perform(post("/notifications/{id}/read", 10L)
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.id").value(10L));

        verify(service).markRead(10L);
    }
}