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

    // Devuelve el DTO completo que espera el front (montoSena, clienteEmail, etc.)
    @GetMapping("/info-sena/{presupuestoId}")
    public ResponseEntity<?> infoSena(@PathVariable Long presupuestoId) {
        Presupuesto p = presupuestoRepo.findById(presupuestoId)
                .orElseThrow(() -> new EntityNotFoundException("Presupuesto no encontrado: " + presupuestoId));

        if (!"APROBADO".equalsIgnoreCase(p.getEstado())) {
            return ResponseEntity.status(409).body(
                    new Msg("El presupuesto no está APROBADO. Estado actual: " + p.getEstado())
            );
        }

        // Usa la lógica centralizada que ya arma el PagoInfoDTO con 9 campos
        PagoInfoDTO dto = gestionService.getPagoInfoPublico(presupuestoId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/cobrar-sena/{presupuestoId}")
    public ResponseEntity<?> cobrarSenaApi(@PathVariable Long presupuestoId,
                                           @RequestBody PagoApiReq req) {
        try {
            System.out.println("[COBRAR-SENA] pid=" + presupuestoId +
                    " pm=" + req.paymentMethodId() +
                    " inst=" + req.installments() +
                    " issuer=" + req.issuerId() +
                    " email=" + req.payerEmail());
            Presupuesto p = gestionService.cobrarSenaApi(presupuestoId, req);
            return ResponseEntity.ok(new PayResp(
                    "ok",
                    p.getSenaPaymentStatus(),
                    p.getSenaPaymentId(),
                    p.getSenaMonto()
            ));
        } catch (Exception e) {
            e.printStackTrace(); // <- para ver stack en consola
            return ResponseEntity.badRequest().body(new Msg(e.getMessage()));
        }
    }

    public record Msg(String message) {}
    public record PayResp(String result, String status, String paymentId, BigDecimal monto) {}
}
