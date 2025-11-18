// src/main/java/ar/edu/utn/tfi/web/AdminPresupuestoController.java
package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.domain.SolicitudPresupuesto;
import ar.edu.utn.tfi.repository.PresupuestoRepository;
import ar.edu.utn.tfi.repository.FacturaMockRepository;
import ar.edu.utn.tfi.service.PresupuestoService;            // SOLICITUDES
import ar.edu.utn.tfi.service.PresupuestoGestionService;     // PRESUPUESTOS
import ar.edu.utn.tfi.web.dto.PresupuestoAdminDTO;
import ar.edu.utn.tfi.web.dto.SolicitudDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/presupuestos")
public class AdminPresupuestoController {

    private final PresupuestoService solicitudesService;          // lista SOLICITUDES
    private final PresupuestoGestionService presupuestosService;  // lista PRESUPUESTOS
    private final FacturaMockRepository facturaRepo;
    private final PresupuestoRepository presupuestoRepo;

    public AdminPresupuestoController(PresupuestoService solicitudesService,
                                      PresupuestoGestionService presupuestosService,
                                      FacturaMockRepository facturaRepo,
                                      PresupuestoRepository presupuestoRepo) {
        this.solicitudesService = solicitudesService;
        this.presupuestosService = presupuestosService;
        this.facturaRepo = facturaRepo;
        this.presupuestoRepo = presupuestoRepo;
    }

    // ---------------- SOLICITUDES ----------------

    @GetMapping("/solicitudes")
    public List<SolicitudDTO> listarSolicitudes(@RequestParam(required = false) String estado) {
        return solicitudesService.listar(estado).stream()
                .map(s -> SolicitudDTO.from(
                        s,
                        presupuestoRepo.existsBySolicitudId(s.getId()) // flag: ya tiene presupuesto o no
                ))
                .toList();
    }

    record NotaReq(String nota) {}

    @PutMapping("/solicitudes/{id}/aprobar")
    public ResponseEntity<?> aprobar(@PathVariable Long id,
                                     @RequestBody(required = false) NotaReq body,
                                     Authentication auth) {
        try {
            SolicitudPresupuesto s = solicitudesService.aprobar(
                    id,
                    auth == null ? "admin" : auth.getName(),
                    body == null ? null : body.nota()
            );
            return ResponseEntity.ok(Map.of("message", "Aprobada", "id", s.getId()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of(
                    "error", "CONFLICT",
                    "message", e.getMessage()
            ));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "NOT_FOUND",
                    "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/solicitudes/{id}/rechazar")
    public ResponseEntity<?> rechazar(@PathVariable Long id,
                                      @RequestBody(required = false) NotaReq body,
                                      Authentication auth) {
        try {
            SolicitudPresupuesto s = solicitudesService.rechazar(
                    id,
                    auth == null ? "admin" : auth.getName(),
                    body == null ? null : body.nota()
            );
            return ResponseEntity.ok(Map.of("message", "Rechazada", "id", s.getId()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of(
                    "error", "CONFLICT",
                    "message", e.getMessage()
            ));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "NOT_FOUND",
                    "message", e.getMessage()
            ));
        }
    }

    // ---------------- PRESUPUESTOS (grilla admin) ----------------

    @GetMapping
    public List<PresupuestoAdminDTO> listar(@RequestParam(required = false) String estado,
                                            @RequestParam(required = false) Long solicitudId) {

        var presupuestos = presupuestosService.listar(estado, solicitudId);

        if (presupuestos.isEmpty()) {
            return List.of();
        }

        var ids = presupuestos.stream().map(p -> p.getId()).toList();

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

    // Obtener una solicitud por id (ADMIN) para autocompletar en admin-presupuestos.html
    @GetMapping("/solicitudes/{id}")
    public SolicitudDTO getSolicitudById(@PathVariable Long id) {
        SolicitudPresupuesto s = solicitudesService.getById(id);
        boolean generado = presupuestoRepo.existsBySolicitudId(s.getId());
        return SolicitudDTO.from(s, generado);
    }
}