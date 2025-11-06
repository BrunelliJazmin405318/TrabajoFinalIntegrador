// src/main/java/ar/edu/utn/tfi/service/IngresosReportService.java
package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.repository.IngresosReportRepository;
import ar.edu.utn.tfi.repository.IngresosReportRepository.IngresoMesTipo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Service
public class IngresosReportService {

    private final IngresosReportRepository repo;

    public IngresosReportService(IngresosReportRepository repo) {
        this.repo = repo;
    }

    public Map<String, Object> ingresosSenasVsFinales(LocalDate from, LocalDate toInclusive) {
        // Defaults: mes actual
        if (from == null || toInclusive == null) {
            YearMonth ym = YearMonth.now();
            from = ym.atDay(1);
            toInclusive = ym.atEndOfMonth();
        }
        if (toInclusive.isBefore(from)) {
            LocalDate tmp = from; from = toInclusive; toInclusive = tmp;
        }

        // Ventana [desde, hasta) => hasta = to+1 día
        LocalDateTime desde = from.atStartOfDay();
        LocalDateTime hasta = toInclusive.plusDays(1).atStartOfDay();

        // Armar eje de meses continuo
        List<String> meses = buildMonthAxis(from, toInclusive); // "YYYY-MM"

        // Mapas acumuladores
        Map<String, BigDecimal> senas = new LinkedHashMap<>();
        Map<String, BigDecimal> finales = new LinkedHashMap<>();
        for (String m : meses) {
            senas.put(m, BigDecimal.ZERO);
            finales.put(m, BigDecimal.ZERO);
        }

        // Traer datos
        List<IngresoMesTipo> rows = repo.listarIngresos(desde, hasta);
        for (IngresoMesTipo r : rows) {
            String mes = r.getMes();
            String tipo = r.getTipo() == null ? "" : r.getTipo().toUpperCase(Locale.ROOT);
            BigDecimal total = r.getTotal() == null ? BigDecimal.ZERO : r.getTotal();

            if (senas.containsKey(mes)) {
                if ("SENA".equals(tipo)) {
                    senas.put(mes, senas.get(mes).add(total));
                } else if ("FINAL".equals(tipo)) {
                    finales.put(mes, finales.get(mes).add(total));
                }
            }
        }

        // Pasar a arrays numéricos
        List<BigDecimal> serieSena   = new ArrayList<>();
        List<BigDecimal> serieFinal  = new ArrayList<>();
        for (String m : meses) {
            serieSena.add(senas.get(m));
            serieFinal.add(finales.get(m));
        }

        // Totales
        BigDecimal totalSena  = serieSena.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFinal = serieFinal.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalGeneral = totalSena.add(totalFinal);

        return Map.of(
                "labels", meses,                 // ["2025-09","2025-10","2025-11", ...]
                "senas", serieSena,              // [BigDecimal,...]
                "finales", serieFinal,           // [BigDecimal,...]
                "totalSena", totalSena,
                "totalFinal", totalFinal,
                "totalGeneral", totalGeneral,
                "from", from.toString(),
                "to", toInclusive.toString()
        );
    }

    private List<String> buildMonthAxis(LocalDate from, LocalDate toInclusive) {
        YearMonth start = YearMonth.from(from);
        YearMonth end   = YearMonth.from(toInclusive);

        List<String> out = new ArrayList<>();
        YearMonth cur = start;
        while (!cur.isAfter(end)) {
            out.add(cur.toString()); // YYYY-MM
            cur = cur.plusMonths(1);
        }
        return out;
    }
}