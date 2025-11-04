// src/main/java/ar/edu/utn/tfi/web/AdminPagoManualController.java
package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.service.PresupuestoGestionService;
import ar.edu.utn.tfi.web.dto.PagoManualReq;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/pagos-manuales")
public class AdminPagoManualController {

    private final PresupuestoGestionService service;

    public AdminPagoManualController(PresupuestoGestionService service) {
        this.service = service;
    }

    @PostMapping("/registrar")
    public ResponseEntity<?> registrar(@RequestBody PagoManualReq req, Authentication auth) {
        var pm = service.registrarPagoManual(req, auth != null ? auth.getName() : "admin");
        return ResponseEntity.ok(Map.of(
                "message", "OK",
                "id", pm.getId(),
                "presupuestoId", pm.getPresupuesto().getId(),
                "tipo", pm.getTipo()
        ));
    }
}