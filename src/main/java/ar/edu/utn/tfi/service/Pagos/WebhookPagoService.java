// src/main/java/ar/edu/utn/tfi/service/Pagos/WebhookPagoService.java
package ar.edu.utn.tfi.service.Pagos;

import ar.edu.utn.tfi.domain.Presupuesto;
import ar.edu.utn.tfi.repository.PresupuestoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class WebhookPagoService {

    private final PresupuestoRepository presupuestoRepo;
    private final WebClient webClient;

    // ⚠ Usar la misma key que en PaymentApiService
    @Value("${mp.api.access-token}")
    private String accessToken;

    public WebhookPagoService(PresupuestoRepository presupuestoRepo, WebClient.Builder builder) {
        this.presupuestoRepo = presupuestoRepo;
        this.webClient = builder.baseUrl("https://api.mercadopago.com").build();
    }

    @Transactional
    public void procesarNotificacion(String topic, String dataId) {
        if (topic == null || topic.isBlank() || dataId == null || dataId.isBlank()) return;

        Long paymentId      = null;
        String paymentStatus= null;
        LocalDateTime paidAt= null;

        // Para mapear al presupuesto (sin preferenceId):
        Long presupuestoId  = null;                   // ← target
        String extRef       = null;                   // external_reference

        if ("merchant_order".equalsIgnoreCase(topic)) {
            // GET /merchant_orders/{id}
            Map<String, Object> mo = getJson("/merchant_orders/" + dataId);
            if (mo == null) return;

            // MP incluye external_reference a nivel de orden
            extRef = getString(mo, "external_reference");
            presupuestoId = tryParsePresupuestoIdFromExternalRef(extRef);

            // Tomamos primer pago (si existe) para status/fecha/id
            List<Map<String, Object>> payments = getListOfMaps(mo, "payments");
            if (payments != null && !payments.isEmpty()) {
                Map<String, Object> primero = payments.get(0);
                paymentId     = getLong(primero, "id");
                paymentStatus = getString(primero, "status");
                paidAt        = toLocalDateTime(getString(primero, "date_approved"));
            }

        } else if ("payment".equalsIgnoreCase(topic)) {
            // GET /v1/payments/{id}
            Map<String, Object> pr = getJson("/v1/payments/" + dataId);
            if (pr == null) return;

            paymentId     = getLong(pr, "id");
            paymentStatus = getString(pr, "status");
            paidAt        = toLocalDateTime(getString(pr, "date_approved"));

            // 1) metadata.presupuesto_id (si lo mandamos al crear el pago)
            Map<String, Object> md = getMap(pr, "metadata");
            if (md != null) {
                Object pid = md.get("presupuesto_id");
                if (pid instanceof Number n) {
                    presupuestoId = n.longValue();
                } else if (pid != null) {
                    try { presupuestoId = Long.parseLong(String.valueOf(pid)); } catch (Exception ignored) {}
                }
            }

            // 2) external_reference = "PRESUPUESTO-123"
            if (presupuestoId == null) {
                extRef = getString(pr, "external_reference");
                presupuestoId = tryParsePresupuestoIdFromExternalRef(extRef);
            }
        } else {
            // Ignorar otros tópicos
            return;
        }

        if (presupuestoId == null) {
            // No podemos mapear el pago al presupuesto: no romper (MP reintenta si fallamos)
            System.err.println("[WEBHOOK-MP] No se pudo resolver presupuestoId (extRef=" + extRef + ")");
            return;
        }

        final Long pid = presupuestoId;

        Presupuesto p = presupuestoRepo.findById(pid)
                .orElseThrow(() -> new EntityNotFoundException("No hay presupuesto id=" + pid));

        // Idempotencia simple
        if ("ACREDITADA".equalsIgnoreCase(Objects.toString(p.getSenaEstado(), "")) &&
                "approved".equalsIgnoreCase(Optional.ofNullable(paymentStatus).orElse(""))) {
            return;
        }

        // Actualizar datos de pago
        p.setSenaPaymentId(paymentId != null ? String.valueOf(paymentId) : null);
        p.setSenaPaymentStatus(paymentStatus);
        p.setSenaPaidAt(paidAt);

        if ("approved".equalsIgnoreCase(Optional.ofNullable(paymentStatus).orElse(""))) {
            p.setSenaEstado("ACREDITADA");
        } else if (p.getSenaEstado() == null || p.getSenaEstado().isBlank()) {
            p.setSenaEstado("PENDIENTE");
        }

        presupuestoRepo.save(p);
    }

    // ───────────────────────── Helpers ─────────────────────────

    private Long tryParsePresupuestoIdFromExternalRef(String extRef) {
        if (extRef == null) return null;
        if (!extRef.startsWith("PRESUPUESTO-")) return null;
        try {
            return Long.parseLong(extRef.substring("PRESUPUESTO-".length()));
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> getJson(String path) {
        try {
            return webClient.get()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            System.err.println("[MP GET " + path + "] " + e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> src, String key) {
        Object o = src.get(key);
        if (o instanceof Map<?, ?> m) return (Map<String, Object>) m;
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getListOfMaps(Map<String, Object> src, String key) {
        Object o = src.get(key);
        if (o instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map) {
            return (List<Map<String, Object>>) o;
        }
        return null;
    }

    private String getString(Map<String, Object> src, String key) {
        Object o = src.get(key);
        return (o != null ? String.valueOf(o) : null);
    }

    private Long getLong(Map<String, Object> src, String key) {
        Object o = src.get(key);
        if (o instanceof Number n) return n.longValue();
        try {
            return (o != null ? Long.parseLong(String.valueOf(o)) : null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime toLocalDateTime(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return OffsetDateTime.parse(iso).toLocalDateTime();
        } catch (Exception e) {
            return null;
        }
    }
}