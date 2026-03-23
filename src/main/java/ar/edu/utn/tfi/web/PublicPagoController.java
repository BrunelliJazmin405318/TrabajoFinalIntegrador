package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.domain.Presupuesto;
import ar.edu.utn.tfi.repository.PresupuestoRepository;
import ar.edu.utn.tfi.service.PresupuestoGestionService;
import ar.edu.utn.tfi.service.PresupuestoService;
import ar.edu.utn.tfi.web.dto.PagoApiReq;
import ar.edu.utn.tfi.web.dto.PagoInfoDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ar.edu.utn.tfi.security.PublicAuth;

import java.math.BigDecimal;

@RestController
@RequestMapping("/public/pagos/api")
public class PublicPagoController {

    private final PresupuestoRepository presupuestoRepo;
    private final PresupuestoGestionService gestionService;
    private final PresupuestoService presupuestoService;

    public PublicPagoController(PresupuestoRepository presupuestoRepo,
                                PresupuestoGestionService gestionService,
                                PresupuestoService presupuestoService) {
        this.presupuestoRepo = presupuestoRepo;
        this.gestionService = gestionService;
        this.presupuestoService = presupuestoService;
    }

    @GetMapping("/info-sena/{presupuestoId}")
    public ResponseEntity<?> infoSena(@PathVariable Long presupuestoId) {

        Long sid = PublicAuth.solicitudIdFromTokenOrNull();
        if (sid == null) {
            return ResponseEntity.status(401).body(new Msg("TOKEN_REQUIRED"));
        }

        Presupuesto p = presupuestoRepo.findById(presupuestoId)
                .orElseThrow(() -> new EntityNotFoundException("Presupuesto no encontrado: " + presupuestoId));

        if (!sid.equals(p.getSolicitudId())) {
            return ResponseEntity.status(403).body(new Msg("FORBIDDEN"));
        }

        if (!"APROBADO".equalsIgnoreCase(p.getEstado())) {
            return ResponseEntity.status(409).body(new Msg("El presupuesto no está APROBADO. Estado actual: " + p.getEstado()));
        }

        PagoInfoDTO dto = gestionService.getPagoInfoPublico(presupuestoId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/cobrar-sena/{presupuestoId}")
    public ResponseEntity<?> cobrarSenaApi(@PathVariable Long presupuestoId,
                                           @RequestBody PagoApiReq req) {
        try {
            Long sid = PublicAuth.solicitudIdFromTokenOrNull();
            if (sid == null) return ResponseEntity.status(401).body(new Msg("TOKEN_REQUIRED"));

            Presupuesto p0 = presupuestoRepo.findById(presupuestoId)
                    .orElseThrow(() -> new EntityNotFoundException("Presupuesto no encontrado: " + presupuestoId));

            if (!sid.equals(p0.getSolicitudId())) {
                return ResponseEntity.status(403).body(new Msg("FORBIDDEN"));
            }

            Presupuesto p = gestionService.cobrarSenaApi(presupuestoId, req);

            return ResponseEntity.ok(new PayResp(
                    "ok",
                    p.getSenaPaymentStatus(),
                    p.getSenaPaymentId(),
                    p.getSenaMonto()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new Msg(e.getMessage()));
        }
    }

    public record Msg(String message) {}
    public record PayResp(String result, String status, String paymentId, BigDecimal monto) {}
}
