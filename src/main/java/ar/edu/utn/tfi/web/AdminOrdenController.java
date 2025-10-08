package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.repository.OrdenTrabajoRepository;
import ar.edu.utn.tfi.service.OrderAdvanceService;
import ar.edu.utn.tfi.service.OrderIrreparableService;
import ar.edu.utn.tfi.service.OrderDelayService;   // <- usar este servicio para demoras
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/ordenes")
public class AdminOrdenController {

    private final OrdenTrabajoRepository ordenRepo;
    private final OrderAdvanceService advanceService;
    private final OrderIrreparableService irreparableService;
    private final OrderDelayService delayService;   // <- reemplaza a DemoraService

    public AdminOrdenController(OrdenTrabajoRepository ordenRepo,
                                OrderAdvanceService advanceService,
                                OrderIrreparableService irreparableService,
                                OrderDelayService delayService) {
        this.ordenRepo = ordenRepo;
        this.advanceService = advanceService;
        this.irreparableService = irreparableService;
        this.delayService = delayService;   // <- asignación correcta
    }

    @PostMapping("/{nro}/avanzar")
    public ResponseEntity<?> avanzarEtapaPorNro(@PathVariable String nro, Authentication auth) {
        var orden = ordenRepo.findByNroOrden(nro)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada: " + nro));
        advanceService.avanzarEtapa(orden.getId(), auth.getName());
        return ResponseEntity.ok(Map.of("message","Etapa avanzada correctamente", "nroOrden", nro));
    }

    @PutMapping("/{nro}/pieza-irreparable")
    public ResponseEntity<?> marcarIrreparable(@PathVariable String nro, Authentication auth) {
        irreparableService.marcarIrreparablePorNro(nro, auth.getName());
        return ResponseEntity.ok(Map.of(
                "message", "Orden marcada como PIEZA_IRREPARABLE",
                "nroOrden", nro
        ));
    }

    // ===== Demoras (NO crea fila nueva: actualiza observación de la etapa SEMI_ARMADO abierta) =====
    record DemoraReq(String motivo, String observacion) {}

    @PostMapping("/{nro}/demora")
    public ResponseEntity<?> registrarDemora(@PathVariable String nro,
                                             @RequestBody DemoraReq body,
                                             Authentication auth) {
        if (body == null || body.motivo() == null || body.motivo().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error","BAD_REQUEST",
                    "message","El campo 'motivo' es obligatorio (FALTA_REPUESTO | AUTORIZACION_CLIENTE | CAPACIDAD_TALLER)"
            ));
        }

        delayService.registrarDemora(
                nro,
                auth.getName(),
                body.motivo().trim().toUpperCase(),
                body.observacion()
        );

        return ResponseEntity.ok(Map.of("message", "Demora registrada", "nroOrden", nro));
    }
}