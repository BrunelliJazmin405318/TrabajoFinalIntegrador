package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.domain.Notificacion;
import ar.edu.utn.tfi.repository.NotificacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificacionService {

    private final NotificacionRepository repo;

    // email -> lista de suscriptores SSE
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public NotificacionService(NotificacionRepository repo) {
        this.repo = repo;
    }

    // =========================================================
    // ======================   SSE   ==========================
    // =========================================================

    /** Cliente se suscribe a notificaciones por email (SSE). */
    public SseEmitter subscribe(String email) {
        SseEmitter emitter = new SseEmitter(0L); // sin timeout
        emitters.computeIfAbsent(email, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(emitter);

        emitter.onCompletion(() -> remove(email, emitter));
        emitter.onTimeout(() -> remove(email, emitter));
        emitter.onError((e) -> remove(email, emitter));
        return emitter;
    }

    private void remove(String email, SseEmitter em) {
        List<SseEmitter> list = emitters.getOrDefault(email, Collections.emptyList());
        list.remove(em);
    }

    private void broadcast(String email, Object payload) {
        if (email == null || email.isBlank()) return;
        List<SseEmitter> list = emitters.get(email);
        if (list == null) return;

        List<SseEmitter> toRemove = new ArrayList<>();
        for (SseEmitter em : list) {
            try {
                em.send(SseEmitter.event().name("notif").data(payload));
            } catch (IOException e) {
                toRemove.add(em);
            }
        }
        list.removeAll(toRemove);
    }

    // =========================================================
    // ============== Persistencia + Envíos Mock ===============
    // =========================================================

    /** Guarda y emite una notificación WEB (in-app) vía SSE. */
    public Notificacion pushWeb(String type, String title, String message,
                                Long ordenId, Long solicitudId, String email) {
        Notificacion n = new Notificacion();
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setOrdenId(ordenId);
        n.setSolicitudId(solicitudId);
        n.setClienteEmail(email);
        n.setChannel("WEB");
        n.setSent(true);
        n.setCreatedAt(LocalDateTime.now());

        Notificacion saved = repo.save(n);

        // emitir a suscriptores SSE de ese email
        broadcast(email, Map.of(
                "id", saved.getId(),
                "type", saved.getType(),
                "title", saved.getTitle(),
                "message", saved.getMessage(),
                "ordenId", saved.getOrdenId(),
                "createdAt", String.valueOf(saved.getCreatedAt())
        ));
        return saved;
    }

    /** Mock de WhatsApp: persiste y muestra por consola. */
    public void pushWhatsappMock(String type, String message,
                                 Long ordenId, String email) {
        Notificacion n = new Notificacion();
        n.setType(type);
        n.setTitle("WhatsApp");
        n.setMessage(message);
        n.setOrdenId(ordenId);
        n.setClienteEmail(email);
        n.setChannel("WHATSAPP");
        n.setSent(true);
        n.setCreatedAt(LocalDateTime.now());
        repo.save(n);

        System.out.println("📲 [WA MOCK] " + (email == null ? "-" : email) + " | " + message);
    }

    /** Listar no leídas (in-app). */
    public List<Notificacion> unread(String email) {
        return repo.findTop20ByClienteEmailAndReadAtIsNullOrderByCreatedAtDesc(email);
    }

    /** Marcar una notificación como leída. */
    public void markRead(Long id) {
        repo.findById(id).ifPresent(n -> {
            n.setReadAt(LocalDateTime.now());
            repo.save(n);
        });
    }

    // =========================================================
    // =====================  HU13 Hook  =======================
    // =========================================================

    /**
     * HU13: Registrar y emitir la notificación cuando una OT
     * pasa a estado LISTO_RETIRAR.
     */
    public void registrarMotorListo(Long ordenId, String clienteEmail, String nroOrden) {
        final String nro = (nroOrden == null || nroOrden.isBlank()) ? "-" : nroOrden;
        final String titulo = "Motor listo para retirar";
        final String mensaje = "Tu motor (OT " + nro + ") está listo para retirar.";

        // 1) In-app (WEB) con SSE
        pushWeb("MOTOR_LISTO", titulo, mensaje, ordenId, null, clienteEmail);

        // 2) WhatsApp (mock)
        pushWhatsappMock("MOTOR_LISTO", mensaje, ordenId, clienteEmail);
    }
}
