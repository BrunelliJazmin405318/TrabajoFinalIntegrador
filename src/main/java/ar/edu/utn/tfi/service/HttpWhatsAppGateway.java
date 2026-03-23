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

    // 👇 NUEVO
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    public HttpWhatsAppGateway(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    // 👇 NUEVO
    private boolean isDev() {
        return "dev".equalsIgnoreCase(activeProfile);
    }

    @Override
    public void send(String telefonoDestino, String mensaje) {

        if (!enabled) {
            if (isDev()) {
                System.out.println("📲 [WA deshabilitado] -> " + mensaje);
            }
            return;
        }

        if (telefonoDestino == null || telefonoDestino.isBlank()) {
            if (isDev()) {
                System.out.println("📲 [WA] sin teléfono destino");
            }
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

            if (isDev()) {
                System.out.println("✅ [WA] enviado correctamente");
            }

        } catch (Exception e) {
            if (isDev()) {
                System.out.println("❌ [WA] error al enviar: " + e.getMessage());
            }
        }
    }
}