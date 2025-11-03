package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.domain.Presupuesto;
import ar.edu.utn.tfi.domain.PresupuestoItem;
import ar.edu.utn.tfi.repository.PresupuestoItemRepository;
import ar.edu.utn.tfi.service.PresupuestoGestionService;
import ar.edu.utn.tfi.web.dto.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PresupuestosController {

    private final PresupuestoGestionService service;
    private final PresupuestoItemRepository itemRepo;

    public PresupuestosController(PresupuestoGestionService service, PresupuestoItemRepository itemRepo) {
        this.service = service;
        this.itemRepo = itemRepo;
    }

    // Requiere autenticación (no está bajo /public)
    @PostMapping("/presupuestos/generar")
    public PresupuestoDTO generar(@RequestBody PresupuestoGenerarReq req) {
        Presupuesto p = service.generarDesdeSolicitud(req.solicitudId(), req.vehiculoTipo(), req.servicios());
        List<PresupuestoItemDTO> items = itemRepo.findByPresupuestoId(p.getId())
                .stream()
                .map(it -> new PresupuestoItemDTO(it.getServicioNombre(), it.getPrecioUnitario()))
                .toList();
        return PresupuestoDTO.from(p, items);
    }

    @GetMapping("/admin/presupuestos")
    public List<PresupuestoListDTO> listar(@RequestParam(required = false) String estado,
                                           @RequestParam(required = false) Long solicitudId) {
        return service.listar(estado, solicitudId).stream()
                .map(p -> new PresupuestoListDTO(
                        p.getId(),
                        p.getSolicitudId(),
                        p.getClienteNombre(),
                        p.getClienteEmail(),
                        p.getVehiculoTipo(),
                        p.getTotal(),
                        p.getEstado(),
                        p.getCreadaEn(),
                        // —— Seña
                        p.getSenaEstado(),

                        p.getSenaMonto(),
                        p.getSenaPaidAt(),
                        p.getSenaPaymentId(),
                        p.getSenaPaymentStatus()
                ))
                .toList();
    }

    @GetMapping("/admin/presupuestos/{id}")
    public PresupuestoDTO ver(@PathVariable Long id) {
        Presupuesto p = service.getById(id);
        List<PresupuestoItemDTO> items = itemRepo.findByPresupuestoId(p.getId())
                .stream()
                .map(it -> new PresupuestoItemDTO(it.getServicioNombre(), it.getPrecioUnitario()))
                .toList();
        return PresupuestoDTO.from(p, items);
    }

    @PutMapping("/admin/presupuestos/{id}/aprobar")
    public ResponseEntity<?> aprobar(@PathVariable Long id,
                                     @RequestBody(required = false) DecisionReq body,
                                     Authentication auth) {
        try {
            Presupuesto p = service.aprobar(id, auth == null ? "admin" : auth.getName(),
                    body == null ? null : body.nota());
            return ResponseEntity.ok().body(new Msg("Aprobado", p.getId()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(new Err("CONFLICT", e.getMessage()));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(404).body(new Err("NOT_FOUND", e.getMessage()));
        }
    }

    @PutMapping("/admin/presupuestos/{id}/rechazar")
    public ResponseEntity<?> rechazar(@PathVariable Long id,
                                      @RequestBody(required = false) DecisionReq body,
                                      Authentication auth) {
        try {
            Presupuesto p = service.rechazar(id, auth == null ? "admin" : auth.getName(),
                    body == null ? null : body.nota());
            return ResponseEntity.ok().body(new Msg("Rechazado", p.getId()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(new Err("CONFLICT", e.getMessage()));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(404).body(new Err("NOT_FOUND", e.getMessage()));
        }
    }

    record Msg(String message, Long id) {}
    record Err(String error, String message) {}

}

