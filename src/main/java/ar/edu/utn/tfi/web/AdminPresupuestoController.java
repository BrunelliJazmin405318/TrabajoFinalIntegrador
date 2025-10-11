package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.domain.SolicitudPresupuesto;
import ar.edu.utn.tfi.repository.SolicitudPresupuestoRepository;
import ar.edu.utn.tfi.service.MailService;
import ar.edu.utn.tfi.service.PresupuestoService;
import ar.edu.utn.tfi.web.dto.SolicitudDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/presupuestos")
public class AdminPresupuestoController {

    private final SolicitudPresupuestoRepository repo;
    private final MailService mailService;
    private final PresupuestoService service; // ⬅️ agregado para listar

    public AdminPresupuestoController(SolicitudPresupuestoRepository repo,
                                      MailService mailService,
                                      PresupuestoService service) { // ⬅️ agregado para listar
        this.repo = repo;
        this.mailService = mailService;
        this.service = service;
    }

    // ⬇️ NUEVO: listado para la grilla de /admin-solicitudes.html
    @GetMapping("/solicitudes")
    public List<SolicitudDTO> listar(@RequestParam(required = false) String estado) {
        return service.listar(estado)
                .stream()
                .map(SolicitudDTO::from)
                .toList();
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
