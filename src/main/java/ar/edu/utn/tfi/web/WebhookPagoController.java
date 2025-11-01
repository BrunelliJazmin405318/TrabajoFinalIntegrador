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
            @RequestBody(required = false) String body
    ) {
        return procesar(type, topic, id, dataId, signature, body);
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
            String body
    ) {
        // Normalizamos claves: a veces viene "type", otras "topic"; y "id" vs "data.id"
        String t = (topic != null && !topic.isBlank()) ? topic : type;
        String d = (dataId != null && !dataId.isBlank()) ? dataId : id;

        // Log liviano para diagnóstico (no romper tiempos de respuesta)
        System.out.println("[WEBHOOK-MP] topic=" + t + " id=" + d);

        try {
            webhookPagoService.procesarNotificacion(t, d);
        } catch (Exception e) {
            // Importante: devolver 200 para que MP no reintente eternamente
            System.err.println("[WEBHOOK-MP][WARN] " + e.getMessage());
        }
        // Devolvemos 200 SIEMPRE y rápido: MP reintenta si no
        return ResponseEntity.ok("ok");
    }
}