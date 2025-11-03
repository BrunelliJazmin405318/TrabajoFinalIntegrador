// src/main/java/ar/edu/utn/tfi/service/Pagos/PaymentApiService.java
package ar.edu.utn.tfi.service.Pagos;

import ar.edu.utn.tfi.web.dto.PagoApiReq;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentApiService {

    private final WebClient mp;
    @Value("${mp.api.access-token}") private String accessToken;

    public PaymentApiService(WebClient.Builder builder) {
        this.mp = builder.baseUrl("https://api.mercadopago.com").build();
    }

    public Map<String, Object> crearPago(Long presupuestoId,
                                         BigDecimal monto,
                                         PagoApiReq req) {

        Map<String, Object> payer = new HashMap<>();
        if (req.payerEmail() != null && !req.payerEmail().isBlank())
            payer.put("email", req.payerEmail());

        Map<String, Object> ident = new HashMap<>();
        if (req.payerDocType() != null && !req.payerDocType().isBlank())
            ident.put("type", req.payerDocType());
        if (req.payerDocNumber() != null && !req.payerDocNumber().isBlank())
            ident.put("number", req.payerDocNumber());
        if (!ident.isEmpty()) payer.put("identification", ident);

        Map<String, Object> body = new HashMap<>();
        body.put("transaction_amount", monto);                     // BigDecimal OK
        body.put("token", req.token());
        body.put("description", "Seña Presupuesto #" + presupuestoId);
        body.put("installments", (req.installments() == null || req.installments() < 1) ? 1 : req.installments());
        if (req.paymentMethodId() != null) body.put("payment_method_id", req.paymentMethodId());
        if (req.issuerId() != null && !req.issuerId().isBlank()) body.put("issuer_id", req.issuerId());
        if (!payer.isEmpty()) body.put("payer", payer);

        // metadatos útiles para mapear luego en webhook
        body.put("external_reference", "PRESUPUESTO-" + presupuestoId);
        Map<String,Object> meta = new HashMap<>();
        meta.put("presupuesto_id", presupuestoId);
        body.put("metadata", meta);

        // Si querés forzar única captura:
        body.put("capture", true);

        try {
            return mp.post()
                    .uri("/v1/payments")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header("X-Idempotency-Key", "sena-"+presupuestoId+"-"+System.currentTimeMillis())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (WebClientResponseException e) {
            String apiMsg = e.getResponseBodyAsString(); // ← cuerpo real de MP
            throw new IllegalArgumentException("Mercado Pago rechazó la solicitud: " + apiMsg, e);
        } catch (Exception e) {
            throw new IllegalStateException("Error comunicando con MP: " + e.getMessage(), e);
        }
    }
}