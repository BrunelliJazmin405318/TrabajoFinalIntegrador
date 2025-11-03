// src/main/java/ar/edu/utn/tfi/web/WebhookPagoController.java
package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.service.Pagos.WebhookPagoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/pagos")
public class WebhookPagoController {

    private final WebhookPagoService webhookPagoService;

    public WebhookPagoController(WebhookPagoService webhookPagoService) {
        this.webhookPagoService = webhookPagoService;
    }

    // MP suele llamar por POST y/o GET. Ambos terminan en el mismo handler.
    @PostMapping("/webhook-mp")
    public ResponseEntity<String> recibirPOST(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "topic", required = false) String topic,
            @RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "data.id", required = false) String dataId,
            @RequestHeader(value = "x-signature", required = false) String signature,
            @RequestHeader(value = "x-request-id", required = false) String requestId,
            @RequestBody(required = false) String body
    ) {
        return procesar(type, topic, id, dataId, signature, requestId);
    }

    @GetMapping("/webhook-mp")
    public ResponseEntity<String> recibirGET(@RequestParam Map<String,String> params) {
        String type   = params.get("type");
        String topic  = params.get("topic");
        String id     = params.get("id");
        String dataId = params.get("data.id");
        return procesar(type, topic, id, dataId, null, null);
    }

    private ResponseEntity<String> procesar(
            String type,
            String topic,
            String id,
            String dataId,
            String signature,
            String requestId
    ) {
        String t = (topic != null && !topic.isBlank()) ? topic : type;
        String d = (dataId != null && !dataId.isBlank()) ? dataId : id;

        System.out.println("[WEBHOOK-MP] topic=" + t + " id=" + d + " x-request-id=" + requestId);

        try {
            webhookPagoService.procesarNotificacion(t, d, requestId);
        } catch (Exception e) {
            // Devolver 200 para que MP no reintente eternamente
            System.err.println("[WEBHOOK-MP][WARN] " + e.getMessage());
        }
        return ResponseEntity.ok("ok");
    }
}