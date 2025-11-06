// src/main/java/ar/edu/utn/tfi/service/ReportesService.java
package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.repository.OrdenTrabajoReportRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ReportesService {

    private final OrdenTrabajoReportRepository ordenReportRepo;

    public ReportesService(OrdenTrabajoReportRepository ordenReportRepo) {
        this.ordenReportRepo = ordenReportRepo;
    }

    public Map<String, Object> motoresVsTapas(LocalDate desde, LocalDate hastaInclusive) {
        LocalDateTime d0 = desde.atStartOfDay();
        LocalDateTime d1 = hastaInclusive.plusDays(1).atStartOfDay();

        long motores = 0, tapas = 0;
        for (var row : ordenReportRepo.contarPorTipoEntre(d0, d1)) {
            String tipo = (row.getTipo() == null ? "" : row.getTipo().toUpperCase());
            if ("MOTOR".equals(tipo)) motores = row.getCnt();
            else if ("TAPA".equals(tipo)) tapas = row.getCnt();
        }
        long total = motores + tapas;
        double ratio = (tapas == 0 ? (motores == 0 ? 0.0 : 1.0 * motores) : (1.0 * motores / tapas));

        return Map.of(
                "motores", motores,
                "tapas", tapas,
                "total", total,
                "ratio", ratio
        );
    }

    public Map<String, Object> motoresPorEtapa(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            YearMonth ym = YearMonth.now();
            from = ym.atDay(1);
            to = ym.atEndOfMonth();
        }
        if (to.isBefore(from)) { var tmp = from; from = to; to = tmp; }

        LocalDateTime d0 = from.atStartOfDay();
        LocalDateTime d1 = to.plusDays(1).atStartOfDay();

        var rows = ordenReportRepo.contarMotoresPorEtapaRango(d0, d1);

        List<String> etapas = new ArrayList<>();
        List<Long> cantidades = new ArrayList<>();
        long total = 0L;

        for (var r : rows) {
            String e = String.valueOf(r.get("etapa"));
            long c = ((Number) r.get("cnt")).longValue();
            etapas.add(e);
            cantidades.add(c);
            total += c;
        }
        return Map.of("etapas", etapas, "cantidades", cantidades, "total", total);
    }
}