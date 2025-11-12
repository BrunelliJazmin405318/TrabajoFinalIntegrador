// src/main/java/ar/edu/utn/tfi/web/ReportesExportController.java
package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.service.ReporteExportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reportes")
public class ReportesExportController {

    private final ReporteExportService exportService;

    public ReportesExportController(ReporteExportService exportService) {
        this.exportService = exportService;
    }

    // GET /api/reportes/export?reporte=clientes|motores-vs-tapas|motores-por-etapa|ingresos-senas-finales
    //     &from=YYYY-MM-DD&to=YYYY-MM-DD&format=xlsx|pdf&top=10
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam String reporte,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "xlsx") String format,
            @RequestParam(required = false, defaultValue = "10") Integer top
    ) {
        String filename = reporte + "-" + from + "_" + to + "." + format.toLowerCase();
        byte[] bytes;

        switch (reporte) {
            case "clientes" -> {
                if ("pdf".equalsIgnoreCase(format))
                    bytes = exportService.clientesFrecuentesPdf(from, to, top);
                else
                    bytes = exportService.clientesFrecuentesXlsx(from, to, top);
            }
            case "motores-vs-tapas" -> {
                if ("pdf".equalsIgnoreCase(format))
                    bytes = exportService.motoresVsTapasPdf(from, to);
                else
                    bytes = exportService.motoresVsTapasXlsx(from, to);
            }
            case "motores-por-etapa" -> {
                if ("pdf".equalsIgnoreCase(format))
                    bytes = exportService.motoresPorEtapaPdf(from, to);
                else
                    bytes = exportService.motoresPorEtapaXlsx(from, to);
            }
            case "ingresos-senas-finales" -> {
                if ("pdf".equalsIgnoreCase(format))
                    bytes = exportService.ingresosSenasFinalesPdf(from, to);
                else
                    bytes = exportService.ingresosSenasFinalesXlsx(from, to);
            }
            default -> throw new IllegalArgumentException("Reporte desconocido: " + reporte);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(filename).build()
        );
        headers.setContentType("pdf".equalsIgnoreCase(format)
                ? MediaType.APPLICATION_PDF
                : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}
