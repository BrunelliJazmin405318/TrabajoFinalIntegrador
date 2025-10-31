package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.service.PresupuestoPagoService;
import ar.edu.utn.tfi.web.dto.LinkPagoDTO;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/pagos")
public class PagosController {

    private final PresupuestoPagoService pagoService;

    public PagosController(PresupuestoPagoService pagoService) {
        this.pagoService = pagoService;
    }

    // HU10: generar link de seña (30%) para un presupuesto APROBADO
    @PostMapping("/link-sena/{presupuestoId}")
    @RolesAllowed("ADMIN") // o @PreAuthorize("hasRole('ADMIN')") según tu config
    public ResponseEntity<LinkPagoDTO> generarLinkSena(@PathVariable Long presupuestoId) throws Exception {
        LinkPagoDTO dto = pagoService.generarLinkSena(presupuestoId);
        return ResponseEntity.ok(dto);
    }
}