package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.domain.Presupuesto;
import ar.edu.utn.tfi.repository.PresupuestoRepository;
import ar.edu.utn.tfi.service.PresupuestoGestionService;
import ar.edu.utn.tfi.web.dto.PagoApiReq;
import ar.edu.utn.tfi.web.dto.PagoInfoDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/public/pagos/api")
public class PublicPagoController {

    private final PresupuestoRepository presupuestoRepo;
    private final PresupuestoGestionService gestionService;

    public PublicPagoController(PresupuestoRepository presupuestoRepo,
                                 PresupuestoGestionService gestionService) {
        this.presupuestoRepo = presupuestoRepo;
        this.gestionService = gestionService;
    }

    @GetMapping("/info-sena/{presupuestoId}")
    public ResponseEntity<?> infoSena(@PathVariable Long presupuestoId) {
        Presupuesto p = presupuestoRepo.findById(presupuestoId)
                .orElseThrow(() -> new EntityNotFoundException("Presupuesto no encontrado: " + presupuestoId));

        if (!"APROBADO".equalsIgnoreCase(p.getEstado())) {
            return ResponseEntity.status(409).body(
                    new Msg("El presupuesto no está APROBADO. Estado actual: " + p.getEstado())
            );
        }

        BigDecimal monto = p.getTotal()
                .multiply(new BigDecimal("0.30"))
                .setScale(2, java.math.RoundingMode.HALF_UP);

        return ResponseEntity.ok(
                new PagoInfoDTO(p.getId(), monto, p.getClienteEmail(), new BigDecimal("0.30"))
        );
    }

    @PostMapping("/cobrar-sena/{presupuestoId}")
    public ResponseEntity<?> cobrarSenaApi(@PathVariable Long presupuestoId,
                                           @RequestBody PagoApiReq req) {
        try {
            Presupuesto p = gestionService.cobrarSenaApi(presupuestoId, req);
            return ResponseEntity.ok(new PayResp(
                    "ok",
                    p.getSenaPaymentStatus(),
                    p.getSenaPaymentId(),
                    p.getSenaMonto()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new Msg(e.getMessage()));
        }
    }

    public record Msg(String message) {}
    public record PayResp(String result, String status, String paymentId, BigDecimal monto) {}
}