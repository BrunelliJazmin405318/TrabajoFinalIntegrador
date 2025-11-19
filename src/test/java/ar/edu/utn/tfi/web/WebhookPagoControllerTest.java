// src/test/java/ar/edu/utn/tfi/web/WebhookPagoControllerTest.java
package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.security.SecurityConfig;
import ar.edu.utn.tfi.service.Pagos.WebhookPagoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WebhookPagoController.class)
@Import(SecurityConfig.class)
@SuppressWarnings("removal")
class WebhookPagoControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    WebhookPagoService webhookPagoService;

    @Test
    @DisplayName("POST /pagos/webhook-mp → llama a procesarNotificacion con topic y data.id y devuelve ok")
    void recibirPOST_ok() throws Exception {
        mvc.perform(post("/pagos/webhook-mp")
                        .param("type", "payment")
                        .param("topic", "merchant_order")
                        .param("id", "999")
                        .param("data.id", "12345")
                        .header("x-signature", "sig-123")
                        .header("x-request-id", "req-1")
                        .content("{}")
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));

        // topic tiene prioridad sobre type, data.id sobre id
        verify(webhookPagoService)
                .procesarNotificacion("merchant_order", "12345", "req-1");
    }

    @Test
    @DisplayName("GET /pagos/webhook-mp → llama a procesarNotificacion con type y id y devuelve ok")
    void recibirGET_ok() throws Exception {
        mvc.perform(get("/pagos/webhook-mp")
                        .param("type", "payment")
                        .param("id", "abc123")
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));

        // cuando no hay topic ni data.id, usa type e id
        verify(webhookPagoService)
                .procesarNotificacion("payment", "abc123", null);
    }
}