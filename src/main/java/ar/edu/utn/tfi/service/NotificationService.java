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

import org.springframework.transaction.annotation.Propagation;
import ar.edu.utn.tfi.domain.SolicitudPresupuesto;
import ar.edu.utn.tfi.domain.Presupuesto;
import ar.edu.utn.tfi.domain.Cliente;
import ar.edu.utn.tfi.repository.SolicitudPresupuestoRepository;

@Service
public class NotificationService {

    private final NotificacionRepository repo;
    private final NotificationHub hub;
    private final SolicitudPresupuestoRepository solicitudRepo;
    private final WhatsAppGateway whatsapp;

    // Registro simple de suscriptores por email
    private final Map<String, SseEmitter> emittersByEmail = new ConcurrentHashMap<>();

    public NotificationService(NotificacionRepository repo,
                               NotificationHub hub,
                               WhatsAppGateway whatsapp,
                               SolicitudPresupuestoRepository solicitudRepo) {
        this.repo = repo;
        this.hub = hub;
        this.whatsapp = whatsapp;
        this.solicitudRepo = solicitudRepo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void emitirListoRetirar(OrdenTrabajo ot, String destinoClienteOpt) {
        String etapa = "LISTO_RETIRAR";

        String link = "http://localhost:8080/consulta.html?nro=" + ot.getNroOrden();

        String msg =
                "Tu orden " + ot.getNroOrden() + " está lista para retirar.\n\n" +
                        "Podés seguir el estado acá:\n" +
                        link;

        // In-App
        var n = new Notificacion();
        n.setOrdenId(ot.getId());
        n.setNroOrden(ot.getNroOrden());
        n.setCanal("IN_APP");
        n.setType(etapa);
        n.setMessage(msg);
        n.setEstado("ENVIADA");
        n.setClienteDestino(destinoClienteOpt);
        n.setTitle("Orden lista para retirar");
        repo.save(n);

        // SSE por nro de orden
        hub.push(
                ot.getNroOrden(),
                "listo-retirar",
                "{\"id\":" + n.getId() + ",\"tipo\":\"LISTO_RETIRAR\",\"mensaje\":\"" + msg + "\"}"
        );

        // WhatsApp
        whatsapp.send(destinoClienteOpt, msg);

        // Registramos la notificación de WhatsApp
        var w = new Notificacion();
        w.setOrdenId(ot.getId());
        w.setNroOrden(ot.getNroOrden());
        w.setCanal("WHATSAPP");
        w.setType(etapa);
        w.setMessage(msg);
        w.setEstado("ENVIADA");
        w.setClienteDestino(destinoClienteOpt);
        w.setTitle("Orden lista para retirar");
        repo.save(w);
    }

    public SseEmitter subscribe(String email) {
        var emitter = new SseEmitter(0L);
        emittersByEmail.put(email, emitter);

        emitter.onCompletion(() -> emittersByEmail.remove(email));
        emitter.onTimeout(() -> emittersByEmail.remove(email));
        emitter.onError((ex) -> emittersByEmail.remove(email));

        try {
            emitter.send(SseEmitter.event().name("ping").data("subscribed:" + email));
        } catch (Exception ignored) {
        }

        return emitter;
    }

    @Transactional(readOnly = true)
    public List<Notificacion> unread(String email) {
        return repo.findTop20ByClienteEmailAndReadAtIsNullOrderByCreatedAtDesc(email);
    }

    @Transactional
    public void markRead(Long id) {
        repo.findById(id).ifPresent(n -> {
            n.setReadAt(LocalDateTime.now());
            repo.save(n);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notificarDecisionSolicitud(SolicitudPresupuesto s) {
        String tel = s.getClienteTelefono();

        String motivo = (s.getDecisionMotivo() == null || s.getDecisionMotivo().isBlank())
                ? "-"
                : s.getDecisionMotivo();

        String msg = switch (s.getEstado()) {
            case "APROBADO" -> "Hola " + s.getClienteNombre()
                    + ". Tu solicitud #" + s.getId() + " fue aprobada. Motivo: " + motivo;
            case "RECHAZADO" -> "Hola " + s.getClienteNombre()
                    + ". Tu solicitud #" + s.getId() + " fue rechazada. Motivo: " + motivo;
            default -> "Hola " + s.getClienteNombre()
                    + ". Tu solicitud #" + s.getId() + " cambió de estado a: " + s.getEstado() + ".";

        };

        whatsapp.send(tel, msg);

        var n = new Notificacion();
        n.setSolicitudId(s.getId());
        n.setCanal("WHATSAPP");
        n.setType("SOLICITUD_" + s.getEstado());
        n.setTitle("Estado de tu solicitud");
        n.setMessage(msg);
        n.setEstado("ENVIADA");
        n.setClienteDestino(tel);
        repo.save(n);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notificarPresupuestoGenerado(Presupuesto p, SolicitudPresupuesto s) {
        String tel = s.getClienteTelefono();
        String linkSolicitud = "http://localhost:8080/public/presupuestos/solicitud/" + s.getId();

        String msg = "Hola " + s.getClienteNombre()
                + ", ya generamos tu presupuesto #" + p.getId()
                + " para la solicitud #" + s.getId()
                + ". Monto estimado: " + p.getTotal()
                + ". Podés verlo acá: " + linkSolicitud;

        whatsapp.send(tel, msg);

        var n = new Notificacion();
        n.setSolicitudId(s.getId());
        n.setCanal("WHATSAPP");
        n.setType("PRESUPUESTO_GENERADO");
        n.setTitle("Presupuesto generado");
        n.setMessage(msg);
        n.setEstado("ENVIADA");
        n.setClienteDestino(tel);
        repo.save(n);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notificarIngresoOrden(OrdenTrabajo ot, Cliente cliente) {
        if (cliente == null) {
            System.out.println("⚠ No se pudo notificar ingreso, cliente null para OT " + ot.getNroOrden());
            return;
        }

        String tel = cliente.getTelefono();
        if (tel == null || tel.isBlank()) {
            System.out.println("⚠ Cliente sin teléfono, no se envía WA para OT " + ot.getNroOrden());
            return;
        }

        String nombre = (cliente.getNombre() == null || cliente.getNombre().isBlank())
                ? ""
                : cliente.getNombre();

        String msg = "Hola " + nombre
                + ". Tu unidad ingresó al taller correctamente. "
                + "Número de orden: " + ot.getNroOrden()
                + ". Te notificaremos cuando esté lista para retirar.";

        whatsapp.send(tel, msg);

        var n = new Notificacion();
        n.setOrdenId(ot.getId());
        n.setNroOrden(ot.getNroOrden());
        n.setCanal("WHATSAPP");
        n.setType("INGRESO_ORDEN");
        n.setTitle("Ingreso al taller");
        n.setMessage(msg);
        n.setEstado("ENVIADA");
        n.setClienteDestino(tel);
        repo.save(n);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notificarDecisionPresupuesto(Presupuesto p) {
        if (p == null) {
            System.out.println("⚠ No se pudo notificar decisión de presupuesto: objeto null");
            return;
        }

        SolicitudPresupuesto s = null;
        if (p.getSolicitudId() != null) {
            s = solicitudRepo.findById(p.getSolicitudId()).orElse(null);
        }

        String tel = null;
        String nombreCliente = null;

        if (s != null) {
            tel = (s.getClienteTelefono() == null ? null : s.getClienteTelefono().trim());
            nombreCliente = s.getClienteNombre();
        }

        if (nombreCliente == null || nombreCliente.isBlank()) {
            nombreCliente = p.getClienteNombre();
        }
        if (nombreCliente == null) {
            nombreCliente = "";
        }

        if (tel == null || tel.isBlank()) {
            System.out.println("⚠ No hay teléfono para notificar decisión de presupuesto #" + p.getId());
            return;
        }

        String motivo = (p.getDecisionMotivo() == null || p.getDecisionMotivo().isBlank())
                ? "-"
                : p.getDecisionMotivo();

        String msg = switch (p.getEstado()) {
            case "APROBADO" -> "Hola " + nombreCliente
                    + ". Tu presupuesto #" + p.getId() + " fue aprobado. Total estimado: " + p.getTotal()
                    + ". Motivo: " + motivo;
            case "RECHAZADO" -> "Hola " + nombreCliente
                    + ". Tu presupuesto #" + p.getId() + " fue rechazado. Motivo: " + motivo;
            default -> "Hola " + nombreCliente
                    + ". Tu presupuesto #" + p.getId() + " cambió de estado a: " + p.getEstado()
                    + ". Motivo: " + motivo;

        };

        whatsapp.send(tel, msg);

        var n = new Notificacion();
        n.setSolicitudId(p.getSolicitudId());
        n.setCanal("WHATSAPP");
        n.setType("PRESUPUESTO_" + p.getEstado());
        n.setTitle("Estado de tu presupuesto");
        n.setMessage(msg);
        n.setEstado("ENVIADA");
        n.setClienteDestino(tel);
        repo.save(n);
    }

    public void notificarPinSolicitud(String telefono, Long solicitudId, String pin, int minutos) {
        String link = "http://localhost:8080/estado-solicitud.html?id=" + solicitudId;

        String mensaje =
                "Hola.\n\n" +
                        "Recibimos tu solicitud #" + solicitudId + ".\n\n" +
                        "Código de acceso: " + pin + "\n\n" +
                        "Podés consultar el estado desde este enlace:\n" +
                        link + "\n\n" +
                        "Ingresá el código cuando el sistema te lo solicite.\n\n" +
                        "El código vence en " + minutos + " minutos.";

        whatsapp.send(telefono, mensaje);
    }

    public void notificarOtCreada(String telefono, Long solicitudId, String nroOrden, String pin, int minutos) {
        String link = "http://localhost:8080/consulta.html?nro=" + nroOrden;

        String mensaje =
                "Hola.\n\n" +
                        "Tu unidad ingresó al taller y se generó la orden de trabajo " + nroOrden + ".\n\n" +
                        "Código de acceso: " + pin + "\n\n" +
                        "Podés seguir el estado desde este enlace:\n" +
                        link + "\n\n" +
                        "Ingresá el código cuando el sistema te lo solicite.\n\n" +
                        "El código vence en " + minutos + " minutos.";

        whatsapp.send(telefono, mensaje);
    }
    public void notificarReenvioPinSolicitud(String telefono, Long solicitudId, String pin, int minutos) {

        String link = "http://localhost:8080/estado-solicitud.html?id=" + solicitudId;

        String mensaje =
                "Hola.\n\n" +
                        "Te enviamos un nuevo código de acceso para consultar tu solicitud #" + solicitudId + ".\n\n" +
                        "Nuevo código: " + pin + "\n\n" +
                        "Ingresá desde este enlace:\n" +
                        link + "\n\n" +
                        "El código vence en " + minutos + " minutos.";

        whatsapp.send(telefono, mensaje);
    }
    public void notificarReenvioPinOrden(String telefono, String nroOrden, String pin, int minutos) {

        String link = "http://localhost:8080/consulta.html?nro=" + nroOrden;

        String mensaje =
                "Hola.\n\n" +
                        "Te enviamos un nuevo código de acceso para consultar la orden " + nroOrden + ".\n\n" +
                        "Nuevo código: " + pin + "\n\n" +
                        "Ingresá desde este enlace:\n" +
                        link + "\n\n" +
                        "El código vence en " + minutos + " minutos.";

        whatsapp.send(telefono, mensaje);
    }
}