// src/main/java/ar/edu/utn/tfi/web/AdminPresupuestoController.java
package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.domain.SolicitudPresupuesto;
import ar.edu.utn.tfi.repository.SolicitudPresupuestoRepository;
import ar.edu.utn.tfi.repository.FacturaMockRepository;     // ⬅️ NUEVO
import ar.edu.utn.tfi.service.MailService;
import ar.edu.utn.tfi.service.PresupuestoService;            // SOLICITUDES
import ar.edu.utn.tfi.service.PresupuestoGestionService;     // PRESUPUESTOS
import ar.edu.utn.tfi.web.dto.PresupuestoAdminDTO;
import ar.edu.utn.tfi.web.dto.SolicitudDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/presupuestos")
public class AdminPresupuestoController {

    private final SolicitudPresupuestoRepository repo;
    private final MailService mailService;

    private final PresupuestoService solicitudesService;       // lista SOLICITUDES
    private final PresupuestoGestionService presupuestosService; // lista PRESUPUESTOS

    private final FacturaMockRepository facturaRepo;           // ⬅️ NUEVO

    public AdminPresupuestoController(SolicitudPresupuestoRepository repo,
                                      MailService mailService,
                                      PresupuestoService solicitudesService,
                                      PresupuestoGestionService presupuestosService,
                                      FacturaMockRepository facturaRepo) {   // ⬅️ NUEVO en ctor
        this.repo = repo;
        this.mailService = mailService;
        this.solicitudesService = solicitudesService;
        this.presupuestosService = presupuestosService;
        this.facturaRepo = facturaRepo;                         // ⬅️ NUEVO
    }

    // ---------------- SOLICITUDES ----------------

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

    // ---------------- PRESUPUESTOS (grilla admin) ----------------

    @GetMapping
    public List<PresupuestoAdminDTO> listar(@RequestParam(required = false) String estado,
                                            @RequestParam(required = false) Long solicitudId) {

        var presupuestos = presupuestosService.listar(estado, solicitudId);

        // si la lista viene vacía, evitamos query inútil
        if (presupuestos.isEmpty()) {
            return List.of();
        }

        var ids = presupuestos.stream().map(p -> p.getId()).toList();

        // repo de facturas inyectado en el service o controller (elegí dónde lo tengas)
        var facturas = facturaRepo.findByPresupuestoIdIn(ids).stream()
                .collect(Collectors.toMap(f -> f.getPresupuesto().getId(), f -> f));

        return presupuestos.stream()
                .map(p -> {
                    var f = facturas.get(p.getId());
                    return PresupuestoAdminDTO.from(
                            p,
                            (f == null ? null : f.getNumero()),
                            (f == null ? null : f.getTipo())
                    );
                })
                .toList();
    }
}