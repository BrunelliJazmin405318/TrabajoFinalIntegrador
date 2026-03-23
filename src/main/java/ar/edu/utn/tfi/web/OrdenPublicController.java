package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.security.PublicAuth;
import ar.edu.utn.tfi.service.AuditoriaService;
import ar.edu.utn.tfi.service.OrderQueryService;
import ar.edu.utn.tfi.service.PresupuestoService;
import ar.edu.utn.tfi.web.dto.AuditoriaDTO;
import ar.edu.utn.tfi.web.dto.OrderStageDTO;
import ar.edu.utn.tfi.web.dto.PublicOrderDetailsDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/public/ordenes")
public class OrdenPublicController {

    private final OrderQueryService service;
    private final AuditoriaService auditoriaService;
    private final PresupuestoService presupuestoService;

    public OrdenPublicController(OrderQueryService service,
                                 AuditoriaService auditoriaService,
                                 PresupuestoService presupuestoService) {
        this.service = service;
        this.auditoriaService = auditoriaService;
        this.presupuestoService = presupuestoService;
    }

    private ResponseEntity<?> validarTokenContraOrden(String nroOrden) {
        Long sidToken = PublicAuth.solicitudIdFromTokenOrNull();
        if (sidToken == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "TOKEN_REQUIRED",
                    "message", "Falta token. Iniciá con PIN para obtener accessToken."
            ));
        }

        Long sidOrden = presupuestoService.getSolicitudIdByNroOrden(nroOrden);
        if (!sidToken.equals(sidOrden)) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "FORBIDDEN",
                    "message", "El token no corresponde a esta orden."
            ));
        }

        return null; // OK
    }

    @GetMapping("/{nroOrden}/estado")
    public ResponseEntity<?> estado(@PathVariable String nroOrden) {
        var error = validarTokenContraOrden(nroOrden);
        if (error != null) return error;
        PublicOrderDetailsDTO dto = service.getPublicDetailsByNro(nroOrden);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{nroOrden}/historial")
    public ResponseEntity<?> historial(@PathVariable String nroOrden) {
        var error = validarTokenContraOrden(nroOrden);
        if (error != null) return error;
        List<OrderStageDTO> list = service.getHistorialByNro(nroOrden);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{nroOrden}/auditoria")
    public ResponseEntity<?> auditoria(@PathVariable String nroOrden) {
        var error = validarTokenContraOrden(nroOrden);
        if (error != null) return error;

        var out = auditoriaService.listarPorNro(nroOrden)
                .stream().map(AuditoriaDTO::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(out);
    }
}