package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.domain.Notificacion;
import ar.edu.utn.tfi.repository.NotificacionRepository;
import ar.edu.utn.tfi.service.NotificationHub;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/public/notificaciones")
public class NotificacionPublicController {

    private final NotificationHub hub;
    private final NotificacionRepository repo;

    public NotificacionPublicController(NotificationHub hub, NotificacionRepository repo) {
        this.hub = hub;
        this.repo = repo;
    }

    // Cliente se suscribe por nro de orden
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String nroOrden) {
        return hub.subscribe(nroOrden.trim());
    }

    // Para cargar últimas (por si el cliente abrió tarde la pestaña)
    @GetMapping("/ultimas")
    public List<Notificacion> ultimas(@RequestParam String nroOrden) {
        return repo.findTop20ByNroOrdenOrderByCreatedAtDesc(nroOrden.trim());
    }

    // Marcar como leída (opcional)
    @PostMapping("/{id}/leida")
    public void marcarLeida(@PathVariable Long id) {
        repo.findById(id).ifPresent(n -> {
            n.setEstado("LEIDA");
            n.setReadAt(java.time.LocalDateTime.now());
            repo.save(n);
        });
    }
}