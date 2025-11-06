// src/main/java/ar/edu/utn/tfi/web/ReportesController.java
package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.repository.OrdenTrabajoReportRepository;
import ar.edu.utn.tfi.service.ReportesService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reportes")
public class ReportesController {

    private final ReportesService service;
    private final OrdenTrabajoReportRepository reportRepo;
    public ReportesController(ReportesService service, OrdenTrabajoReportRepository reportRepo) {
        this.service = service;
        this.reportRepo = reportRepo;
    }

    @GetMapping("/motores-vs-tapas")
    public Map<String, Object> motoresVsTapas(
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (from == null || to == null) {
            // Default: mes actual
            YearMonth ym = YearMonth.now();
            from = ym.atDay(1);
            to = ym.atEndOfMonth();
        }
        if (to.isBefore(from)) {
            // swap por si vienen invertidas
            var tmp = from; from = to; to = tmp;
        }
        return service.motoresVsTapas(from, to);
    }
    // ReportesController.java
    @GetMapping("/motores-por-etapa")
    public Map<String, Object> motoresPorEtapa(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to",   required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.motoresPorEtapa(from, to);
    }

}