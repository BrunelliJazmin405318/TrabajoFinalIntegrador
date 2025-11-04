// PublicFacturaController.java
package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.service.FacturaMockService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public/facturas")
public class PublicFacturaController {

    private final FacturaMockService facturaService;

    public PublicFacturaController(FacturaMockService facturaService) {
        this.facturaService = facturaService;
    }

    /**
     * Descarga de factura por ID de SOLICITUD (público).
     * Si no existe, la genera con tipo por defecto (query param ?tipo=A|B, default B).
     */
    @GetMapping("/pdf/by-solicitud/{solicitudId}")
    public ResponseEntity<byte[]> pdfBySolicitud(@PathVariable Long solicitudId,
                                                 @RequestParam(defaultValue = "B") String tipo) {
        try {
            byte[] pdf = facturaService.renderPdfBySolicitud(solicitudId, tipo);
            String filename = "factura-solicitud-" + solicitudId + ".pdf";

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(pdf);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(("""
                           {"error":"NOT_FOUND","message":"%s"}
                           """.formatted(e.getMessage())).getBytes());
        } catch (IllegalStateException e) {
            // por ejemplo: final no acreditado aún
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(("""
                           {"error":"CONFLICT","message":"%s"}
                           """.formatted(e.getMessage())).getBytes());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("""
                           {"error":"INTERNAL_ERROR","message":"%s"}
                           """.formatted(e.getMessage())).getBytes());
        }
    }
}