package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.domain.FacturaMock;
import ar.edu.utn.tfi.domain.Presupuesto;
import ar.edu.utn.tfi.domain.PresupuestoItem;
import ar.edu.utn.tfi.repository.FacturaMockRepository;
import ar.edu.utn.tfi.repository.PresupuestoItemRepository;
import ar.edu.utn.tfi.repository.PresupuestoRepository;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FacturaMockService {

    private final PresupuestoRepository presupuestoRepo;
    private final FacturaMockRepository facturaRepo;
    private final PresupuestoItemRepository itemRepo;
    private final MailService mailService;

    public FacturaMockService(PresupuestoRepository presupuestoRepo,
                              FacturaMockRepository facturaRepo,
                              PresupuestoItemRepository itemRepo,
                              MailService mailService) {
        this.presupuestoRepo = presupuestoRepo;
        this.facturaRepo = facturaRepo;
        this.itemRepo = itemRepo;
        this.mailService = mailService;
    }

    /**
     * Genera la factura (A/B) para un presupuesto cuyo FINAL está ACREDITADO.
     * Si ya existe una factura para ese presupuesto, la devuelve (idempotente).
     */
    @Transactional
    public FacturaMock generar(Long presupuestoId, String tipo) {
        var p = presupuestoRepo.findById(presupuestoId)
                .orElseThrow(() -> new EntityNotFoundException("Presupuesto no encontrado: " + presupuestoId));

        if (!"ACREDITADA".equalsIgnoreCase(p.getFinalEstado())) {
            throw new IllegalStateException("Solo se puede facturar si el pago FINAL está acreditado.");
        }

        if (facturaRepo.findByPresupuestoId(presupuestoId).isPresent()) {
            throw new IllegalStateException("Ya existe una factura para este presupuesto.");
        }

        // Generar número mock (FA-000001 / FB-000001)
        String prefijo = tipo.equalsIgnoreCase("A") ? "FA-" : "FB-";
        long count = facturaRepo.count() + 1;
        String numero = String.format("%s%06d", prefijo, count);

        FacturaMock f = new FacturaMock();
        f.setPresupuesto(p);
        f.setTipo(tipo.toUpperCase());
        f.setNumero(numero);
        f.setFechaEmision(LocalDateTime.now());
        f.setTotal(p.getTotal());
        f.setClienteNombre(p.getClienteNombre());
        f.setClienteEmail(p.getClienteEmail());

        // ✅ Guardamos la factura
        f = facturaRepo.save(f);

        // ✅ Enviamos notificación por correo
        mailService.enviarFacturaEmitida(f);

        return f;
    }

    /** Devuelve la factura si existe; si no, la crea (tipo por defecto). */
    @Transactional
    public FacturaMock getOrCreateByPresupuesto(Long presupuestoId, String tipoDefault) {
        return facturaRepo.findByPresupuestoId(presupuestoId)
                .orElseGet(() -> generar(presupuestoId, (tipoDefault == null ? "B" : tipoDefault)));
    }

    /** Render PDF por ID de factura existente. */
    @Transactional(readOnly = true)
    public byte[] renderPdf(Long facturaId) {
        FacturaMock f = facturaRepo.findById(facturaId)
                .orElseThrow(() -> new EntityNotFoundException("Factura no encontrada: " + facturaId));
        Presupuesto p = f.getPresupuesto();
        List<PresupuestoItem> items = itemRepo.findByPresupuestoId(p.getId());
        return buildPdf(f, p, items);
    }

    /**
     * Render PDF asegurando factura para un presupuesto:
     * si no existe, la crea (tipo por defecto B) y luego genera el PDF.
     */
    @Transactional
    public byte[] renderPdfByPresupuesto(Long presupuestoId, String tipoDefault) {
        FacturaMock f = getOrCreateByPresupuesto(presupuestoId, tipoDefault);
        Presupuesto p = f.getPresupuesto();
        List<PresupuestoItem> items = itemRepo.findByPresupuestoId(p.getId());
        return buildPdf(f, p, items);
    }

    // ======================= PDF builder (OpenPDF) =======================

    private byte[] buildPdf(FacturaMock f, Presupuesto p, List<PresupuestoItem> items) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 54, 36);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            var titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            var small = FontFactory.getFont(FontFactory.HELVETICA, 10);
            var normal = FontFactory.getFont(FontFactory.HELVETICA, 12);

            // Encabezado
            Paragraph encabezado = new Paragraph("Rectificadora — Demo Académica\n\n", titleFont);
            encabezado.setAlignment(Element.ALIGN_LEFT);
            doc.add(encabezado);

            // Cuadro de datos de factura
            PdfPTable tInfo = new PdfPTable(new float[]{1.2f, 3f});
            tInfo.setWidthPercentage(100);

            addRow(tInfo, "Tipo:", f.getTipo(), small, normal);
            addRow(tInfo, "Número:", f.getNumero(), small, normal);
            addRow(tInfo, "Fecha emisión:", safeDateTime(f.getFechaEmision()), small, normal);
            addRow(tInfo, "Presupuesto ID:", String.valueOf(p.getId()), small, normal);
            addRow(tInfo, "Cliente:", nv(f.getClienteNombre()), small, normal);
            addRow(tInfo, "Email:", nv(f.getClienteEmail()), small, normal);
            doc.add(tInfo);

            doc.add(Chunk.NEWLINE);

            // Items
            PdfPTable tItems = new PdfPTable(new float[]{4f, 2f});
            tItems.setWidthPercentage(100);

            PdfPCell h1 = new PdfPCell(new Phrase("Concepto", small));
            PdfPCell h2 = new PdfPCell(new Phrase("Precio", small));
            h1.setBackgroundColor(new Color(240, 240, 240));
            h2.setBackgroundColor(new Color(240, 240, 240));
            tItems.addCell(h1);
            tItems.addCell(h2);

            BigDecimal total = BigDecimal.ZERO;
            if (items != null && !items.isEmpty()) {
                for (PresupuestoItem it : items) {
                    tItems.addCell(new Phrase(nv(it.getServicioNombre()), normal));
                    tItems.addCell(new Phrase(money(it.getPrecioUnitario()), normal));
                    if (it.getPrecioUnitario() != null) total = total.add(it.getPrecioUnitario());
                }
            } else {
                // Fallback: sin items, mostramos total del presupuesto
                tItems.addCell(new Phrase("Servicios según presupuesto", normal));
                tItems.addCell(new Phrase(money(p.getTotal()), normal));
                total = p.getTotal() == null ? BigDecimal.ZERO : p.getTotal();
            }
            doc.add(tItems);

            doc.add(Chunk.NEWLINE);

            // Total
            Paragraph tot = new Paragraph("TOTAL: " + money(total), titleFont);
            tot.setAlignment(Element.ALIGN_RIGHT);
            doc.add(tot);

            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("Documento no válido como factura fiscal. Uso académico.", small));

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF: " + e.getMessage(), e);
        }
    }

    private static void addRow(PdfPTable table, String k, String v, Font kf, Font vf) {
        PdfPCell c1 = new PdfPCell(new Phrase(k, kf));
        PdfPCell c2 = new PdfPCell(new Phrase(nv(v), vf));
        c1.setBorder(Rectangle.NO_BORDER);
        c2.setBorder(Rectangle.NO_BORDER);
        table.addCell(c1);
        table.addCell(c2);
    }

    private static String nv(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }

    private static String safeDateTime(LocalDateTime dt) {
        return (dt == null) ? "-" : dt.toString().replace('T', ' ');
    }

    private static String money(BigDecimal n) {
        if (n == null) return "$ 0,00";
        // Formato simple con coma decimal estilo AR
        String raw = String.format("%,.2f", n);
        // Usa coma para decimales y punto para miles
        return "$ " + raw.replace(',', 'X').replace('.', ',').replace('X', '.');
    }
    // FacturaMockService.java (agregar)
    @Transactional
    public byte[] renderPdfBySolicitud(Long solicitudId, String tipoDefault) {
        // Trae el último presupuesto APROBADO de la solicitud
        Presupuesto p = presupuestoRepo
                .findFirstBySolicitudIdAndEstadoOrderByCreadaEnDesc(solicitudId, "APROBADO")
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "No hay presupuesto APROBADO para la solicitud " + solicitudId));

        // Asegura factura (si no existe la crea con el tipo por defecto) y genera el PDF
        FacturaMock f = getOrCreateByPresupuesto(p.getId(), (tipoDefault == null ? "B" : tipoDefault));
        List<PresupuestoItem> items = itemRepo.findByPresupuestoId(p.getId());
        return buildPdf(f, p, items);
    }
}