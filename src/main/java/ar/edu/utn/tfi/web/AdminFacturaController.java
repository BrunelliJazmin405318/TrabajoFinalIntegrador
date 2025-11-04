package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.domain.FacturaMock;
import ar.edu.utn.tfi.service.FacturaMockService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/facturas")
public class AdminFacturaController {

    private final FacturaMockService service;

    public AdminFacturaController(FacturaMockService service) {
        this.service = service;
    }

    // ===== Helper de errores consistentes =====
    private record Err(String error, String message) {}

    // ===== DTO para generación =====
    public record FacturaReq(String tipo) {}

    /**
     * Genera la factura A/B para un presupuesto con FINAL acreditado.
     * Idempotente: si ya existe, devuelve la existente.
     */
    @PostMapping("/generar/{presupuestoId}")
    public ResponseEntity<?> generar(@PathVariable Long presupuestoId,
                                     @RequestBody(required = false) FacturaReq req) {
        try {
            String tipo = (req == null || req.tipo() == null || req.tipo().isBlank())
                    ? "B" : req.tipo().trim().toUpperCase();
            FacturaMock f = service.generar(presupuestoId, tipo);
            return ResponseEntity.ok(f);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(new Err("CONFLICT", e.getMessage()));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(404).body(new Err("NOT_FOUND", e.getMessage()));
        }
    }

    /**
     * Descarga PDF por ID de factura existente.
     */
    @GetMapping(value = "/{facturaId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdfByFactura(@PathVariable Long facturaId) {
        try {
            byte[] pdf = service.renderPdf(facturaId);
            String filename = "factura-" + facturaId + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(404)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                    .body(("Factura no encontrada: " + facturaId).getBytes());
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                    .body(("No se pudo generar el PDF: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Descarga PDF por presupuesto. Si no existe la factura, la crea (tipo ?tipo=A|B, default B).
     * Útil para el botón "Descargar factura" en el panel admin.
     */
    @GetMapping(value = "/pdf/by-presupuesto/{presupuestoId}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdfByPresupuesto(@PathVariable Long presupuestoId,
                                                   @RequestParam(required = false) String tipo) {
        try {
            String t = (tipo == null || tipo.isBlank()) ? "B" : tipo.trim().toUpperCase();
            byte[] pdf = service.renderPdfByPresupuesto(presupuestoId, t);
            String filename = "factura-presupuesto-" + presupuestoId + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                    .body(e.getMessage().getBytes());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(404)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                    .body(e.getMessage().getBytes());
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                    .body(("No se pudo generar el PDF: " + e.getMessage()).getBytes());
        }
    }
}