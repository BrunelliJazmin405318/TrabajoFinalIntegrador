package ar.edu.utn.tfi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class HttpWhatsAppGateway implements WhatsAppGateway {

    private final WebClient webClient;

    @Value("${wa.api.enabled:true}")
    private boolean enabled;

    @Value("${wa.api.base-url}")
    private String baseUrl;

    @Value("${wa.api.token}")
    private String token;

    @Value("${wa.api.from-number}")
    private String fromNumber;

    public HttpWhatsAppGateway(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    @Override
    public void send(String telefonoDestino, String mensaje) {
        if (!enabled) {
            System.out.println("📲 [WA deshabilitado] -> " + telefonoDestino + " | " + mensaje);
            return;
        }
        if (telefonoDestino == null || telefonoDestino.isBlank()) {
            System.out.println("📲 [WA] sin teléfono destino. Mensaje: " + mensaje);
            return;
        }

        try {
            Map<String, Object> payload = Map.of(
                    "from", fromNumber,
                    "to", telefonoDestino.trim(),
                    "message", mensaje
            );

            webClient.post()
                    .uri(baseUrl + "/messages")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            System.out.println("✅ [WA] enviado a " + telefonoDestino + " | " + mensaje);
        } catch (Exception e) {
            System.out.println("❌ [WA] error al enviar a " + telefonoDestino + ": " + e.getMessage());
        }
    }
}