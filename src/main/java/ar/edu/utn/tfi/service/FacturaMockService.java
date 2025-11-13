package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.domain.FacturaMock;
import ar.edu.utn.tfi.domain.Presupuesto;
import ar.edu.utn.tfi.domain.PresupuestoItem;
import ar.edu.utn.tfi.repository.FacturaMockRepository;
import ar.edu.utn.tfi.repository.PresupuestoItemRepository;
import ar.edu.utn.tfi.repository.PresupuestoRepository;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.draw.LineSeparator;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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
     * Genera la factura (A/B/C) para un presupuesto cuyo FINAL está ACREDITADO.
     * Si ya existe una factura para ese presupuesto, lanza error (idempotente).
     */
    @Transactional
    public FacturaMock generar(Long presupuestoId, String tipo) {
        Presupuesto p = presupuestoRepo.findById(presupuestoId)
                .orElseThrow(() -> new EntityNotFoundException("Presupuesto no encontrado: " + presupuestoId));

        if (!"ACREDITADA".equalsIgnoreCase(p.getFinalEstado())) {
            throw new IllegalStateException("Solo se puede facturar si el pago FINAL está acreditado.");
        }

        if (facturaRepo.findByPresupuestoId(presupuestoId).isPresent()) {
            throw new IllegalStateException("Ya existe una factura para este presupuesto.");
        }

        // Normalizar y validar tipo: A / B / C
        String t = (tipo == null ? "" : tipo.trim().toUpperCase());
        if (!t.matches("[ABC]")) {
            throw new IllegalArgumentException("Tipo de factura inválido. Debe ser A, B o C.");
        }

        // Prefijo según tipo
        String prefijo;
        switch (t) {
            case "A" -> prefijo = "FA-";
            case "B" -> prefijo = "FB-";
            case "C" -> prefijo = "FC-";
            default -> prefijo = "F-";
        }

        long count = facturaRepo.count() + 1;
        String numero = String.format("%s%06d", prefijo, count);

        FacturaMock f = new FacturaMock();
        f.setPresupuesto(p);
        f.setTipo(t);
        f.setNumero(numero);
        f.setFechaEmision(LocalDateTime.now());
        f.setTotal(p.getTotal());
        f.setClienteNombre(p.getClienteNombre());
        f.setClienteEmail(p.getClienteEmail());

        // Guardar factura
        f = facturaRepo.save(f);

        // Notificación por mail (mock)
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
     * si no existe, la crea (tipo por defecto) y luego genera el PDF.
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

            Font titleFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font bigLetter   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26);
            Font normal      = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font small       = FontFactory.getFont(FontFactory.HELVETICA, 8);
            Font boldSmall   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            Font barcodeFont = FontFactory.getFont(FontFactory.COURIER, 9);

            // ───────────────── ORIGINAL (esquina superior derecha) ─────────────────
            Paragraph original = new Paragraph("ORIGINAL", small);
            original.setAlignment(Element.ALIGN_RIGHT);
            doc.add(original);

            // ───────────────── ENCABEZADO CON LOGO + DATOS + LETRA ─────────────────
            PdfPTable head = new PdfPTable(new float[]{3f, 1.2f});
            head.setWidthPercentage(100);

            // Columna izquierda: logo + datos empresa
            PdfPCell left = new PdfPCell();
            left.setBorder(Rectangle.BOX);
            left.setPadding(6f);

            try {
                Image logo = Image.getInstance(
                        Objects.requireNonNull(
                                getClass().getResource("/static/img/logo-fondo-transparente.png")
                        )
                );
                logo.scaleToFit(180, 80);   // ⬅️ tamaño del logo
                logo.setAlignment(Image.LEFT);
                left.addElement(logo);
            } catch (Exception ex) {
                // si no se encuentra el logo, no rompemos el PDF
            }

            Paragraph empName = new Paragraph("RECTIFICADORA CORNEJO SAS", titleFont);
            empName.setSpacingBefore(4f);
            left.addElement(empName);
            left.addElement(new Phrase("""
            CUIT: 30-00000000-0
            Domicilio: Ferdinand de Lesseps 1000 - Córdoba
            Condición frente al IVA: Responsable Inscripto
            """, small));
            head.addCell(left);

            // Columna derecha: recuadro con letra A/B/C
            PdfPCell right = new PdfPCell();
            right.setBorder(Rectangle.BOX);
            right.setPaddingTop(8f);
            right.setPaddingBottom(8f);
            right.setHorizontalAlignment(Element.ALIGN_CENTER);

            Paragraph letra = new Paragraph(f.getTipo(), bigLetter);
            letra.setAlignment(Element.ALIGN_CENTER);
            right.addElement(letra);

            Paragraph txtFactura = new Paragraph("FACTURA " + f.getTipo(), normal);
            txtFactura.setAlignment(Element.ALIGN_CENTER);
            right.addElement(txtFactura);

            Paragraph cod = new Paragraph("COD. " + codComprobante(f.getTipo()), small);
            cod.setAlignment(Element.ALIGN_CENTER);
            right.addElement(cod);

            head.addCell(right);
            doc.add(head);

            doc.add(Chunk.NEWLINE);

            // ───────────────── DATOS GENERALES FACTURA ─────────────────
            PdfPTable tInfo = new PdfPTable(new float[]{1.2f, 3f});
            tInfo.setWidthPercentage(100);

            addRow(tInfo, "Número:", f.getNumero(), small, normal);
            addRow(tInfo, "Fecha emisión:", safeDateTime(f.getFechaEmision()), small, normal);
            addRow(tInfo, "Presupuesto ID:", String.valueOf(p.getId()), small, normal);
            addRow(tInfo, "Cliente:", nv(f.getClienteNombre()), small, normal);
            addRow(tInfo, "Email:", nv(f.getClienteEmail()), small, normal);

            String condIvaCliente = switch (f.getTipo()) {
                case "A" -> "Responsable Inscripto";
                case "B" -> "Consumidor Final / Exento";
                case "C" -> "Consumidor Final";
                default -> "-";
            };
            addRow(tInfo, "Condición frente al IVA (cliente):", condIvaCliente, small, normal);

            doc.add(tInfo);
            doc.add(Chunk.NEWLINE);

            // ───────────────── ITEMS ─────────────────
            PdfPTable tItems = new PdfPTable(new float[]{4f, 2f});
            tItems.setWidthPercentage(100);

            PdfPCell h1 = new PdfPCell(new Phrase("Concepto", small));
            PdfPCell h2 = new PdfPCell(new Phrase("Precio", small));
            h1.setBackgroundColor(new Color(240, 240, 240));
            h2.setBackgroundColor(new Color(240, 240, 240));
            tItems.addCell(h1);
            tItems.addCell(h2);

            BigDecimal totalNeto = BigDecimal.ZERO;
            if (items != null && !items.isEmpty()) {
                for (PresupuestoItem it : items) {
                    tItems.addCell(new Phrase(nv(it.getServicioNombre()), normal));
                    tItems.addCell(new Phrase(money(it.getPrecioUnitario()), normal));
                    if (it.getPrecioUnitario() != null) {
                        totalNeto = totalNeto.add(it.getPrecioUnitario());
                    }
                }
            } else {
                tItems.addCell(new Phrase("Servicios según presupuesto", normal));
                tItems.addCell(new Phrase(money(p.getTotal()), normal));
                totalNeto = p.getTotal() == null ? BigDecimal.ZERO : p.getTotal();
            }
            doc.add(tItems);

            doc.add(Chunk.NEWLINE);

            // ───────────────── LÍNEA DIVISORIA ITEMS / TOTALES ─────────────────
            PdfPTable sep = new PdfPTable(1);
            sep.setWidthPercentage(100);
            PdfPCell sepCell = new PdfPCell(new Phrase(""));
            sepCell.setBorder(Rectangle.TOP);
            sepCell.setBorderWidthTop(0.5f);
            sepCell.setFixedHeight(4f);
            sep.addCell(sepCell);
            doc.add(sep);

            doc.add(Chunk.NEWLINE);

            // ───────────────── TOTALES SEGÚN TIPO ─────────────────
            if (totalNeto == null) totalNeto = BigDecimal.ZERO;

            BigDecimal iva21 = BigDecimal.ZERO;
            BigDecimal totalConIva = totalNeto;

            String tipo = f.getTipo() == null ? "" : f.getTipo().trim().toUpperCase();
            if ("A".equals(tipo)) {
                iva21 = totalNeto.multiply(new BigDecimal("0.21"))
                        .setScale(2, RoundingMode.HALF_UP);
                totalConIva = totalNeto.add(iva21);
            } else {
                totalConIva = totalNeto;
            }

            PdfPTable totTable = new PdfPTable(new float[]{3f, 1f});
            totTable.setWidthPercentage(60);
            totTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            if ("A".equals(tipo)) {
                addTotalRow(totTable, "Importe neto gravado:", money(totalNeto), small, normal);
                addTotalRow(totTable, "IVA 21%:", money(iva21), small, normal);
                addTotalRow(totTable, "Importe total:", money(totalConIva), small, titleFont);
            } else if ("B".equals(tipo)) {
                addTotalRow(totTable, "Subtotal (IVA incluido):", money(totalConIva), small, normal);
                addTotalRow(totTable, "Importe total:", money(totalConIva), small, titleFont);
            } else { // C
                addTotalRow(totTable, "Importe total:", money(totalConIva), small, titleFont);
            }

            doc.add(totTable);
            doc.add(Chunk.NEWLINE);

            // Leyendas según tipo
            if ("C".equals(tipo)) {
                Paragraph leyendaC = new Paragraph(
                        "Responsable Monotributo. Operación sin discriminación de IVA.",
                        small
                );
                doc.add(leyendaC);
                doc.add(Chunk.NEWLINE);
            } else if ("B".equals(tipo)) {
                Paragraph leyendaB = new Paragraph(
                        "Factura B emitida a consumidor final / exento. El importe incluye el IVA correspondiente.",
                        small
                );
                doc.add(leyendaB);
                doc.add(Chunk.NEWLINE);
            }

            // ───────────────── CAE + “CÓDIGO DE BARRAS” MOCK ─────────────────
            doc.add(Chunk.NEWLINE);

            PdfPTable caeTable = new PdfPTable(new float[]{3f, 2f});
            caeTable.setWidthPercentage(100);

            String caeN = "000000000000000";  // mock
            String caeVto = "31/12/2099";     // mock

            PdfPCell caeLeft = new PdfPCell();
            caeLeft.setBorder(Rectangle.NO_BORDER);
            caeLeft.addElement(new Phrase("CAE N°: " + caeN, small));
            caeLeft.addElement(new Phrase("Fecha de vto. de CAE: " + caeVto, small));
            caeTable.addCell(caeLeft);

            PdfPCell caeRight = new PdfPCell();
            caeRight.setBorder(Rectangle.NO_BORDER);
            caeRight.setHorizontalAlignment(Element.ALIGN_RIGHT);

            Paragraph fakeBarcode = new Paragraph("|| | ||| || || ||| | || ||| ||", barcodeFont);
            fakeBarcode.setAlignment(Element.ALIGN_RIGHT);
            caeRight.addElement(fakeBarcode);

            caeTable.addCell(caeRight);
            doc.add(caeTable);

            // ───────────────── LEYENDA FINAL DEMO ─────────────────
            Paragraph aviso = new Paragraph(
                    "Documento no válido como factura fiscal. Uso académico — Sistema de Gestión Rectificadora Demo.",
                    boldSmall
            );
            aviso.setSpacingBefore(8f);
            doc.add(aviso);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF: " + e.getMessage(), e);
        }
    }

    // ======================= HELPERS =======================

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
        String raw = String.format("%,.2f", n);
        return "$ " + raw.replace(',', 'X').replace('.', ',').replace('X', '.');
    }

    // Código AFIP mock para cada tipo de factura
    private static String codComprobante(String tipo) {
        return switch (tipo == null ? "" : tipo.trim().toUpperCase()) {
            case "A" -> "01";
            case "B" -> "06";
            case "C" -> "011";
            default -> "--";
        };
    }

    // Fila de totales alineada a la derecha
    private static void addTotalRow(PdfPTable table, String label, String value, Font kf, Font vf) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, kf));
        PdfPCell c2 = new PdfPCell(new Phrase(value, vf));
        c1.setBorder(Rectangle.NO_BORDER);
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c1);
        table.addCell(c2);
    }

    @Transactional(readOnly = true)
    public byte[] renderPdfBySolicitud(Long solicitudId) {
        Presupuesto p = presupuestoRepo
                .findFirstBySolicitudIdAndEstadoOrderByCreadaEnDesc(solicitudId, "APROBADO")
                .orElseThrow(() -> new EntityNotFoundException(
                        "No hay presupuesto APROBADO para la solicitud " + solicitudId));

        FacturaMock f = facturaRepo.findByPresupuestoId(p.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Todavía no hay una factura emitida para esta solicitud."));

        List<PresupuestoItem> items = itemRepo.findByPresupuestoId(p.getId());
        return buildPdf(f, p, items);
    }
}