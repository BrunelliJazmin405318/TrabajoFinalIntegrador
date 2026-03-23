// PublicFacturaController.java
package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.service.FacturaMockService;
import ar.edu.utn.tfi.service.PresupuestoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import ar.edu.utn.tfi.security.PublicAuth;

@RestController
@RequestMapping("/public/facturas")
public class PublicFacturaController {

    private final FacturaMockService facturaService;
    private final PresupuestoService presupuestoService;

    public PublicFacturaController(FacturaMockService facturaService,
                                   PresupuestoService presupuestoService) {
        this.facturaService = facturaService;
        this.presupuestoService = presupuestoService;
    }

    /**
     * Descarga de factura por ID de SOLICITUD (público).
     * Si no existe, la genera con tipo por defecto (query param ?tipo=A|B, default B).
     */
    @GetMapping("/pdf/by-solicitud/{solicitudId}")
    public ResponseEntity<byte[]> pdfBySolicitud(@PathVariable Long solicitudId) {

        Long sid = PublicAuth.solicitudIdFromTokenOrNull();
        if (sid == null) {
            return ResponseEntity.status(401).body(("""
           {"error":"TOKEN_REQUIRED","message":"Falta token"}
           """).getBytes());
        }
        if (!sid.equals(solicitudId)) {
            return ResponseEntity.status(403).body(("""
           {"error":"FORBIDDEN","message":"Token no corresponde a esta solicitud"}
           """).getBytes());
        }

        try {
            byte[] pdf = facturaService.renderPdfBySolicitud(solicitudId);
            String filename = "factura-solicitud-" + solicitudId + ".pdf";

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(pdf);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(("""
           {"error":"NOT_FOUND","message":"%s"}
           """.formatted(e.getMessage())).getBytes());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(("""
           {"error":"CONFLICT","message":"%s"}
           """.formatted(e.getMessage())).getBytes());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(("""
           {"error":"INTERNAL_ERROR","message":"%s"}
           """.formatted(e.getMessage())).getBytes());
        }
    }
}