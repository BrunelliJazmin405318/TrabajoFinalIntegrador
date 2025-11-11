package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.domain.Notificacion;
import ar.edu.utn.tfi.domain.OrdenTrabajo;
import ar.edu.utn.tfi.repository.NotificacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificacionRepository repo;
    private final NotificationHub hub;

    public NotificationService(NotificacionRepository repo, NotificationHub hub) {
        this.repo = repo;
        this.hub = hub;
    }

    @Transactional
    public void emitirListoRetirar(OrdenTrabajo ot, String destinoClienteOpt) {
        // 1) Persistimos IN-APP
        var msg = "Tu orden " + ot.getNroOrden() + " está lista para retirar.";
        var n = new Notificacion();
        n.setOrdenId(ot.getId());
        n.setNroOrden(ot.getNroOrden());
        n.setCanal("IN_APP");
        n.setTipo("LISTO_RETIRAR");
        n.setMensaje(msg);
        n.setEstado("ENVIADA");
        n.setClienteDestino(destinoClienteOpt);
        repo.save(n);

        // 2) Emitimos SSE a suscriptores del nro de orden (con ID + mensaje)
        hub.push(
                ot.getNroOrden(),
                "listo-retirar",
                "{\"id\":" + n.getId() + ",\"tipo\":\"LISTO_RETIRAR\",\"mensaje\":\"" + msg + "\"}"
        );

        // 3) Mock WhatsApp (más adelante reemplazamos por API real)
        System.out.println("📲 [MOCK WhatsApp] a " + (destinoClienteOpt == null ? "(desconocido)" : destinoClienteOpt)
                + " | " + msg);
        var w = new Notificacion();
        w.setOrdenId(ot.getId());
        w.setNroOrden(ot.getNroOrden());
        w.setCanal("WHATSAPP");
        w.setTipo("LISTO_RETIRAR");
        w.setMensaje(msg);
        w.setEstado("ENVIADA"); // si la API real falla, poner ERROR
        w.setClienteDestino(destinoClienteOpt);
        repo.save(w);
    }
}