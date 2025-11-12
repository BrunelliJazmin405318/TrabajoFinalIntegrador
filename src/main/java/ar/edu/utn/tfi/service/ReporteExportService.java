// src/main/java/ar/edu/utn/tfi/service/ReporteExportService.java
package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.web.dto.ClienteRankingDTO;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ReporteExportService {

    private final ReportesService reportesService;
    private final IngresosReportService ingresosReportService;

    public ReporteExportService(ReportesService reportesService,
                                IngresosReportService ingresosReportService) {
        this.reportesService = reportesService;
        this.ingresosReportService = ingresosReportService;
    }

    // =========================================================
    //                CLIENTES FRECUENTES
    // =========================================================

    public byte[] clientesFrecuentesXlsx(LocalDate from, LocalDate to, int top) {
        List<ClienteRankingDTO> rows = reportesService.rankingClientesFrecuentes(from, to, top);

        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sh = wb.createSheet("Clientes frecuentes");
            int r = 0;

            org.apache.poi.ss.usermodel.Row header = sh.createRow(r++);
            header.createCell(0).setCellValue("#");
            header.createCell(1).setCellValue("Cliente");
            header.createCell(2).setCellValue("Teléfono");
            header.createCell(3).setCellValue("Órdenes");

            int i = 1;
            for (ClienteRankingDTO x : rows) {
                org.apache.poi.ss.usermodel.Row row = sh.createRow(r++);
                row.createCell(0).setCellValue(i++);

                String nombre = x.clienteNombre() == null ? "" : x.clienteNombre();
                String telefono = x.telefono() == null ? "" : x.telefono();
                long cant = x.cantOrdenes() == null ? 0L : x.cantOrdenes();

                row.createCell(1).setCellValue(nombre);
                row.createCell(2).setCellValue(telefono);
                row.createCell(3).setCellValue(cant);
            }

            for (int c = 0; c <= 3; c++) sh.autoSizeColumn(c);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] clientesFrecuentesPdf(LocalDate from, LocalDate to, int top) {
        List<ClienteRankingDTO> rows = reportesService.rankingClientesFrecuentes(from, to, top);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 30, 30, 20, 20);
            PdfWriter.getInstance(doc, out);
            doc.open();

            doc.add(new Paragraph("Clientes frecuentes (" + from + " → " + to + ") Top " + top));
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{8, 42, 30, 20});

            addHeader(table, "#");
            addHeader(table, "Cliente");
            addHeader(table, "Teléfono");
            addHeader(table, "Órdenes");

            int i = 1;
            for (ClienteRankingDTO x : rows) {
                String nombre = x.clienteNombre() == null ? "" : x.clienteNombre();
                String telefono = x.telefono() == null ? "" : x.telefono();
                long cant = x.cantOrdenes() == null ? 0L : x.cantOrdenes();

                table.addCell(String.valueOf(i++));
                table.addCell(nombre);
                table.addCell(telefono);
                table.addCell(String.valueOf(cant));
            }

            doc.add(table);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // =========================================================
    //                MOTORES VS TAPAS
    // =========================================================

    public byte[] motoresVsTapasXlsx(LocalDate from, LocalDate to) {
        Map<String, Object> data = reportesService.motoresVsTapas(from, to);
        long motores = ((Number) data.getOrDefault("motores", 0)).longValue();
        long tapas = ((Number) data.getOrDefault("tapas", 0)).longValue();

        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sh = wb.createSheet("Motores vs Tapas");
            int r = 0;

            org.apache.poi.ss.usermodel.Row h = sh.createRow(r++);
            h.createCell(0).setCellValue("Tipo");
            h.createCell(1).setCellValue("Cantidad");

            org.apache.poi.ss.usermodel.Row r1 = sh.createRow(r++);
            r1.createCell(0).setCellValue("Motores");
            r1.createCell(1).setCellValue(motores);

            org.apache.poi.ss.usermodel.Row r2 = sh.createRow(r++);
            r2.createCell(0).setCellValue("Tapas");
            r2.createCell(1).setCellValue(tapas);

            sh.autoSizeColumn(0);
            sh.autoSizeColumn(1);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] motoresVsTapasPdf(LocalDate from, LocalDate to) {
        Map<String, Object> data = reportesService.motoresVsTapas(from, to);
        long motores = ((Number) data.getOrDefault("motores", 0)).longValue();
        long tapas = ((Number) data.getOrDefault("tapas", 0)).longValue();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document d = new Document(PageSize.A4);
            PdfWriter.getInstance(d, out);
            d.open();

            d.add(new Paragraph("Motores vs Tapas (" + from + " → " + to + ")"));
            d.add(new Paragraph(" "));

            PdfPTable t = new PdfPTable(2);
            t.setWidthPercentage(60);
            addHeader(t, "Tipo");
            addHeader(t, "Cantidad");

            t.addCell("Motores");
            t.addCell(String.valueOf(motores));
            t.addCell("Tapas");
            t.addCell(String.valueOf(tapas));

            d.add(t);
            d.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // =========================================================
    //                MOTORES POR ETAPA
    // =========================================================

    public byte[] motoresPorEtapaXlsx(LocalDate from, LocalDate to) {
        Map<String, Object> data = reportesService.motoresPorEtapa(from, to);

        @SuppressWarnings("unchecked")
        List<String> etapas = (List<String>) data.getOrDefault("etapas", List.of());
        @SuppressWarnings("unchecked")
        List<Number> cantidades = (List<Number>) data.getOrDefault("cantidades", List.of());

        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sh = wb.createSheet("Motores por etapa");
            int r = 0;

            org.apache.poi.ss.usermodel.Row h = sh.createRow(r++);
            h.createCell(0).setCellValue("Etapa");
            h.createCell(1).setCellValue("Cantidad");

            for (int i = 0; i < etapas.size(); i++) {
                org.apache.poi.ss.usermodel.Row row = sh.createRow(r++);
                row.createCell(0).setCellValue(etapas.get(i));

                long val = (i < cantidades.size() && cantidades.get(i) != null)
                        ? cantidades.get(i).longValue()
                        : 0L;
                row.createCell(1).setCellValue(val);
            }

            sh.autoSizeColumn(0);
            sh.autoSizeColumn(1);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] motoresPorEtapaPdf(LocalDate from, LocalDate to) {
        Map<String, Object> data = reportesService.motoresPorEtapa(from, to);

        @SuppressWarnings("unchecked")
        List<String> etapas = (List<String>) data.getOrDefault("etapas", List.of());
        @SuppressWarnings("unchecked")
        List<Number> cantidades = (List<Number>) data.getOrDefault("cantidades", List.of());

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document d = new Document(PageSize.A4);
            PdfWriter.getInstance(d, out);
            d.open();

            d.add(new Paragraph("Motores por etapa (" + from + " → " + to + ")"));
            d.add(new Paragraph(" "));
            PdfPTable t = new PdfPTable(2);
            t.setWidthPercentage(70);
            addHeader(t, "Etapa");
            addHeader(t, "Cantidad");

            for (int i = 0; i < etapas.size(); i++) {
                long val = (i < cantidades.size() && cantidades.get(i) != null)
                        ? cantidades.get(i).longValue()
                        : 0L;

                t.addCell(etapas.get(i));
                t.addCell(String.valueOf(val));
            }

            d.add(t);
            d.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // =========================================================
    //        INGRESOS: SEÑAS VS FINALES  (NUEVO)
    // =========================================================

    public byte[] ingresosSenasFinalesXlsx(LocalDate from, LocalDate to) {
        Map<String,Object> data = ingresosReportService.ingresosSenasVsFinales(from, to);

        @SuppressWarnings("unchecked")
        List<String> labels = (List<String>) data.getOrDefault("labels", List.of());
        @SuppressWarnings("unchecked")
        List<Number> senas = (List<Number>) data.getOrDefault("senas", List.of());
        @SuppressWarnings("unchecked")
        List<Number> finales = (List<Number>) data.getOrDefault("finales", List.of());

        BigDecimal totalSena      = (BigDecimal) data.getOrDefault("totalSena", BigDecimal.ZERO);
        BigDecimal totalFinal     = (BigDecimal) data.getOrDefault("totalFinal", BigDecimal.ZERO);
        BigDecimal totalGeneral   = (BigDecimal) data.getOrDefault("totalGeneral", BigDecimal.ZERO);

        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sh = wb.createSheet("Ingresos seña vs final");
            int r = 0;

            org.apache.poi.ss.usermodel.Row h = sh.createRow(r++);
            h.createCell(0).setCellValue("Mes");
            h.createCell(1).setCellValue("Señas");
            h.createCell(2).setCellValue("Finales");
            h.createCell(3).setCellValue("Total");

            for (int i = 0; i < labels.size(); i++) {
                org.apache.poi.ss.usermodel.Row row = sh.createRow(r++);
                row.createCell(0).setCellValue(labels.get(i));

                double sena   = (i < senas.size()   && senas.get(i)   != null) ? senas.get(i).doubleValue()   : 0.0;
                double fin    = (i < finales.size() && finales.get(i) != null) ? finales.get(i).doubleValue() : 0.0;

                row.createCell(1).setCellValue(sena);
                row.createCell(2).setCellValue(fin);
                row.createCell(3).setCellValue(sena + fin);
            }

            // Fila de totales
            org.apache.poi.ss.usermodel.Row tot = sh.createRow(r++);
            tot.createCell(0).setCellValue("Totales");
            tot.createCell(1).setCellValue(totalSena.doubleValue());
            tot.createCell(2).setCellValue(totalFinal.doubleValue());
            tot.createCell(3).setCellValue(totalGeneral.doubleValue());

            for (int c = 0; c <= 3; c++) sh.autoSizeColumn(c);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] ingresosSenasFinalesPdf(LocalDate from, LocalDate to) {
        Map<String,Object> data = ingresosReportService.ingresosSenasVsFinales(from, to);

        @SuppressWarnings("unchecked")
        List<String> labels = (List<String>) data.getOrDefault("labels", List.of());
        @SuppressWarnings("unchecked")
        List<Number> senas = (List<Number>) data.getOrDefault("senas", List.of());
        @SuppressWarnings("unchecked")
        List<Number> finales = (List<Number>) data.getOrDefault("finales", List.of());

        BigDecimal totalSena      = (BigDecimal) data.getOrDefault("totalSena", BigDecimal.ZERO);
        BigDecimal totalFinal     = (BigDecimal) data.getOrDefault("totalFinal", BigDecimal.ZERO);
        BigDecimal totalGeneral   = (BigDecimal) data.getOrDefault("totalGeneral", BigDecimal.ZERO);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document d = new Document(PageSize.A4.rotate(), 30, 30, 20, 20);
            PdfWriter.getInstance(d, out);
            d.open();

            d.add(new Paragraph("Ingresos: Señas vs Finales (" + from + " → " + to + ")"));
            d.add(new Paragraph(" "));

            PdfPTable t = new PdfPTable(4);
            t.setWidthPercentage(100);
            t.setWidths(new float[]{20, 25, 25, 30});

            addHeader(t, "Mes");
            addHeader(t, "Señas");
            addHeader(t, "Finales");
            addHeader(t, "Total");

            for (int i = 0; i < labels.size(); i++) {
                double sena   = (i < senas.size()   && senas.get(i)   != null) ? senas.get(i).doubleValue()   : 0.0;
                double fin    = (i < finales.size() && finales.get(i) != null) ? finales.get(i).doubleValue() : 0.0;

                t.addCell(labels.get(i));
                t.addCell(formatMoney(sena));
                t.addCell(formatMoney(fin));
                t.addCell(formatMoney(sena + fin));
            }

            // Totales
            PdfPCell totalCell = new PdfPCell(new Phrase("Totales",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            t.addCell(totalCell);
            t.addCell(formatMoney(totalSena.doubleValue()));
            t.addCell(formatMoney(totalFinal.doubleValue()));
            t.addCell(formatMoney(totalGeneral.doubleValue()));

            d.add(t);
            d.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String formatMoney(double v){
        return String.format(Locale.ROOT, "%.2f", v);
    }

    // =========================================================
    //                HELPERS
    // =========================================================

    private static void addHeader(PdfPTable t, String text) {
        PdfPCell c = new PdfPCell(new Phrase(text,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
        c.setBackgroundColor(new Color(230, 230, 230));
        t.addCell(c);
    }
}
