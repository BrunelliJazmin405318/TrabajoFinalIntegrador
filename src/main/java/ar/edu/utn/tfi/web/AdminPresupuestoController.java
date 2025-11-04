package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.domain.SolicitudPresupuesto;
import ar.edu.utn.tfi.repository.SolicitudPresupuestoRepository;
import ar.edu.utn.tfi.service.MailService;
import ar.edu.utn.tfi.service.PresupuestoService;              // <-- tu servicio que lista SOLICITUDES
import ar.edu.utn.tfi.service.PresupuestoGestionService;       // <-- servicio que lista PRESUPUESTOS
import ar.edu.utn.tfi.web.dto.PresupuestoAdminDTO;
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

    // ⚠️ Este servicio es el que ya usabas para SOLICITUDES
    private final PresupuestoService solicitudesService;

    // ✅ Este es el nuevo servicio para PRESUPUESTOS (entidades Presupuesto)
    private final PresupuestoGestionService presupuestosService;

    public AdminPresupuestoController(SolicitudPresupuestoRepository repo,
                                      MailService mailService,
                                      PresupuestoService solicitudesService,
                                      PresupuestoGestionService presupuestosService) {
        this.repo = repo;
        this.mailService = mailService;
        this.solicitudesService = solicitudesService;
        this.presupuestosService = presupuestosService;
    }

    // ---------------- SOLICITUDES (lo que ya tenías) ----------------

    @GetMapping("/solicitudes")
    public List<SolicitudDTO> listarSolicitudes(@RequestParam(required = false) String estado) {
        return solicitudesService.listar(estado)
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

        mailService.enviarDecisionSolicitud(s);
        return ResponseEntity.ok(Map.of("message", "Rechazada", "id", s.getId()));
    }

    // ---------------- PRESUPUESTOS (nuevo endpoint) ----------------

    @GetMapping
    public List<PresupuestoAdminDTO> listar(@RequestParam(required = false) String estado,
                                            @RequestParam(required = false) Long solicitudId) {
        // 🔥 Ahora sí: esto devuelve List<Presupuesto>, por eso compila el map(PresupuestoAdminDTO::from)
        return presupuestosService.listar(estado, solicitudId)
                .stream()
                .map(PresupuestoAdminDTO::from)
                .toList();
    }
}