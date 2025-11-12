package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.domain.Notificacion;
import ar.edu.utn.tfi.domain.OrdenTrabajo;
import ar.edu.utn.tfi.repository.NotificacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationService {

    private final NotificacionRepository repo;
    private final NotificationHub hub;

    // Registro simple de suscriptores por email
    private final Map<String, SseEmitter> emittersByEmail = new ConcurrentHashMap<>();

    public NotificationService(NotificacionRepository repo, NotificationHub hub) {
        this.repo = repo;
        this.hub = hub;
    }

    @Transactional
    public void emitirListoRetirar(OrdenTrabajo ot, String destinoClienteOpt) {
        String etapa = "LISTO_RETIRAR";  // etapa actual

        var msg = "Tu orden " + ot.getNroOrden() + " está lista para retirar.";

        // In-App
        var n = new Notificacion();
        n.setOrdenId(ot.getId());
        n.setNroOrden(ot.getNroOrden());
        n.setCanal("IN_APP");
        n.setType(etapa);
        n.setMessage(msg);
        n.setEstado("ENVIADA");
        n.setClienteDestino(destinoClienteOpt);

        // título según etapa
        if ("LISTO_RETIRAR".equals(etapa)) {
            n.setTitle("Orden lista para retirar");
        } else {
            n.setTitle("Avance de etapa: " + etapa);
        }

        repo.save(n);

        // SSE a suscriptores del nro de orden
        hub.push(
                ot.getNroOrden(),
                "listo-retirar",
                "{\"id\":" + n.getId() + ",\"tipo\":\"LISTO_RETIRAR\",\"mensaje\":\"" + msg + "\"}"
        );

        // Mock WhatsApp
        System.out.println("📲 [MOCK WhatsApp] a " + (destinoClienteOpt == null ? "(desconocido)" : destinoClienteOpt)
                + " | " + msg);

        var w = new Notificacion();
        w.setOrdenId(ot.getId());
        w.setNroOrden(ot.getNroOrden());
        w.setCanal("WHATSAPP");
        w.setType(etapa);
        w.setMessage(msg);
        w.setEstado("ENVIADA");
        w.setClienteDestino(destinoClienteOpt);

        // título según etapa para el registro de WhatsApp
        if ("LISTO_RETIRAR".equals(etapa)) {
            w.setTitle("Orden lista para retirar");
        } else {
            w.setTitle("Avance de etapa: " + etapa);
        }

        repo.save(w);
    }

    // ───────────── Métodos requeridos por el Controller ─────────────

    /** Devuelve (y registra) un SseEmitter para ese email. */
    public SseEmitter subscribe(String email) {
        var emitter = new SseEmitter(0L); // sin timeout
        emittersByEmail.put(email, emitter);

        emitter.onCompletion(() -> emittersByEmail.remove(email));
        emitter.onTimeout(() -> emittersByEmail.remove(email));
        emitter.onError((ex) -> emittersByEmail.remove(email));

        // Enviamos un "ping" inicial opcional
        try {
            emitter.send(SseEmitter.event().name("ping").data("subscribed:" + email));
        } catch (Exception ignored) {}

        return emitter;
    }

    /** Devuelve últimas 20 notificaciones no leídas del usuario. */
    @Transactional(readOnly = true)
    public List<Notificacion> unread(String email) {
        return repo.findTop20ByClienteEmailAndReadAtIsNullOrderByCreatedAtDesc(email);
    }

    /** Marca una notificación como leída (read_at = now). */
    @Transactional
    public void markRead(Long id) {
        repo.findById(id).ifPresent(n -> {
            n.setReadAt(LocalDateTime.now());
            repo.save(n);
        });
    }
}
