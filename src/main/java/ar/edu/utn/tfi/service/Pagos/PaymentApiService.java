package ar.edu.utn.tfi.service.Pagos;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

// src/main/java/ar/edu/utn/tfi/service/Pagos/PaymentApiService.java
@Service
public class PaymentApiService {

    private final WebClient webClient;

    @Value("${mp.api.access-token}")
    private String accessToken;

    public PaymentApiService(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("https://api.mercadopago.com").build();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> crearPago(Long presupuestoId,
                                         BigDecimal amount,
                                         String description,
                                         String token,
                                         String paymentMethodId,
                                         Integer installments,
                                         String issuerId,
                                         String payerEmail) {

        Map<String, Object> body = new HashMap<>();
        body.put("transaction_amount", amount);
        body.put("description", description);
        body.put("payment_method_id", paymentMethodId);
        body.put("token", token);
        body.put("installments", (installments == null ? 1 : installments));

        Map<String, Object> payer = new HashMap<>();
        payer.put("email", payerEmail);
        body.put("payer", payer);

        if (issuerId != null && !issuerId.isBlank()) {
            body.put("issuer_id", issuerId);
        }

        // ✨ CLAVE PARA EL WEBHOOK (API):
        body.put("external_reference", "PRESUPUESTO-" + presupuestoId);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("presupuesto_id", presupuestoId);
        body.put("metadata", metadata);

        return webClient.post()
                .uri("/v1/payments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }
}