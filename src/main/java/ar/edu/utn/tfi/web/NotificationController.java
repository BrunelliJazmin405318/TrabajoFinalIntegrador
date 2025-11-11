package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.domain.Notificacion;
import ar.edu.utn.tfi.service.NotificacionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificacionService service;

    public NotificationController(NotificacionService service) {
        this.service = service;
    }

    // Suscripción SSE
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String email) {
        return service.subscribe(email.trim().toLowerCase());
    }

    // Unread iniciales (para cuando abre la página)
    @GetMapping("/unread")
    public List<Notificacion> unread(@RequestParam String email) {
        return service.unread(email.trim().toLowerCase());
    }

    // Marcar como leída
    @PostMapping("/{id}/read")
    public Map<String, Object> read(@PathVariable Long id) {
        service.markRead(id);
        return Map.of("ok", true, "id", id);
    }
}

