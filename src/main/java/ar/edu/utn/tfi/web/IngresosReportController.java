package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.service.IngresosReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/reportes")
public class IngresosReportController {

    private final IngresosReportService service;

    public IngresosReportController(IngresosReportService service) {
        this.service = service;
    }

    // GET /api/reportes/ingresos-senas-vs-finales?from=2025-08-01&to=2025-11-30
    @GetMapping("/ingresos-senas-vs-finales")
    public Map<String, Object> ingresos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return service.ingresosSenasVsFinales(from, to);
    }
}