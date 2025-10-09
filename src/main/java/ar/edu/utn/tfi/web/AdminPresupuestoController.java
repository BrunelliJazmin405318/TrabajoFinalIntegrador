package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.domain.SolicitudPresupuesto;
import ar.edu.utn.tfi.repository.SolicitudPresupuestoRepository;
import ar.edu.utn.tfi.service.MailService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/admin/presupuestos")
public class AdminPresupuestoController {

    private final SolicitudPresupuestoRepository repo;
    private final MailService mailService;

    public AdminPresupuestoController(SolicitudPresupuestoRepository repo, MailService mailService) {
        this.repo = repo;
        this.mailService = mailService;
    }

    record NotaReq(String nota) {}

    @PutMapping("/solicitudes/{id}/aprobar")
    public ResponseEntity<?> aprobar(@PathVariable Long id,
                                     @RequestBody(required = false) NotaReq body,
                                     Authentication auth) {
        SolicitudPresupuesto s = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada: " + id));

        if (!"PENDIENTE".equals(s.getEstado())) {
            return ResponseEntity.status(409).body(Map.of(
                    "error", "CONFLICT",
                    "message", "Solo se puede aprobar si está PENDIENTE"
            ));
        }

        s.setEstado("APROBADO");
        s.setDecisionUsuario(auth == null ? "admin" : auth.getName());
        s.setDecisionFecha(LocalDateTime.now());
        s.setDecisionMotivo(body == null ? null : body.nota());
        repo.save(s);

        // ⬇️ Enviar email con MailHog
        mailService.enviarDecisionSolicitud(s);

        return ResponseEntity.ok(Map.of("message", "Aprobada", "id", s.getId()));
    }

    @PutMapping("/solicitudes/{id}/rechazar")
    public ResponseEntity<?> rechazar(@PathVariable Long id,
                                      @RequestBody(required = false) NotaReq body,
                                      Authentication auth) {
        SolicitudPresupuesto s = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada: " + id));

        if (!"PENDIENTE".equals(s.getEstado())) {
            return ResponseEntity.status(409).body(Map.of(
                    "error", "CONFLICT",
                    "message", "Solo se puede rechazar si está PENDIENTE"
            ));
        }

        s.setEstado("RECHAZADO");
        s.setDecisionUsuario(auth == null ? "admin" : auth.getName());
        s.setDecisionFecha(LocalDateTime.now());
        s.setDecisionMotivo(body == null ? null : body.nota());
        repo.save(s);

        // ⬇️ Enviar email con MailHog
        mailService.enviarDecisionSolicitud(s);

        return ResponseEntity.ok(Map.of("message", "Rechazada", "id", s.getId()));
    }
}