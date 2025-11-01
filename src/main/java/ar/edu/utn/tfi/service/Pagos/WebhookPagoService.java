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

    @Value("${MP_ACCESS_TOKEN}")
    private String accessToken;

    public WebhookPagoService(PresupuestoRepository presupuestoRepo, WebClient.Builder builder) {
        this.presupuestoRepo = presupuestoRepo;
        this.webClient = builder
                .baseUrl("https://api.mercadopago.com")
                .build();
    }

    @Transactional
    public void procesarNotificacion(String topic, String dataId) {
        if (topic == null || dataId == null) return;

        String preferenceId = null;
        Long paymentId = null;
        String paymentStatus = null;
        LocalDateTime paidAt = null;

        // ─── Caso merchant_order ────────────────────────
        if ("merchant_order".equalsIgnoreCase(topic)) {
            Map<String, Object> mo = getJson("/merchant_orders/" + dataId);
            if (mo == null) return;

            preferenceId = getString(mo, "preference_id");

            List<Map<String, Object>> payments = getListOfMaps(mo, "payments");
            if (payments != null && !payments.isEmpty()) {
                Map<String, Object> primero = payments.get(0);
                paymentId = getLong(primero, "id");
                paymentStatus = getString(primero, "status");
                paidAt = toLocalDateTime(getString(primero, "date_approved"));
            }

            // ─── Caso payment ───────────────────────────────
        } else if ("payment".equalsIgnoreCase(topic)) {
            Map<String, Object> pr = getJson("/v1/payments/" + dataId);
            if (pr == null) return;

            paymentId = getLong(pr, "id");
            paymentStatus = getString(pr, "status");
            paidAt = toLocalDateTime(getString(pr, "date_approved"));

            Map<String, Object> order = getMap(pr, "order");
            if (order != null && order.get("id") != null) {
                String orderId = String.valueOf(order.get("id"));
                Map<String, Object> mo = getJson("/merchant_orders/" + orderId);
                if (mo != null) {
                    preferenceId = getString(mo, "preference_id");
                }
            }
        } else {
            // ignoramos otros tópicos
            return;
        }

        // ─── Si no hay preference, no procesamos ────────
        if (preferenceId == null) return;

        // Hacemos una copia final para usar dentro del lambda
        final String prefId = preferenceId;

        Presupuesto p = presupuestoRepo.findBySenaPreferenceId(prefId)
                .orElseThrow(() -> new EntityNotFoundException("No hay presupuesto con preferencia: " + prefId));

        // ─── Idempotencia ───────────────────────────────
        if ("ACREDITADA".equalsIgnoreCase(Objects.toString(p.getSenaEstado(), ""))) return;

        // ─── Acreditar ──────────────────────────────────
        if ("approved".equalsIgnoreCase(Optional.ofNullable(paymentStatus).orElse(""))) {
            p.setSenaEstado("ACREDITADA");
            p.setSenaPaymentId(paymentId != null ? String.valueOf(paymentId) : null);
            p.setSenaPaymentStatus(paymentStatus);
            p.setSenaPaidAt(paidAt);
            presupuestoRepo.save(p);
        }
    }

    // ───────────────────────── Helpers ─────────────────────────

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
        return o != null ? String.valueOf(o) : null;
    }

    private Long getLong(Map<String, Object> src, String key) {
        Object o = src.get(key);
        if (o instanceof Number n) return n.longValue();
        try {
            return o != null ? Long.parseLong(String.valueOf(o)) : null;
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
