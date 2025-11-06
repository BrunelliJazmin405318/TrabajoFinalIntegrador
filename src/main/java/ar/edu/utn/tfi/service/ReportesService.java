// src/main/java/ar/edu/utn/tfi/service/ReportesService.java
package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.repository.OrdenTrabajoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ReportesService {

    private final OrdenTrabajoRepository ordenRepo;

    public ReportesService(OrdenTrabajoRepository ordenRepo) {
        this.ordenRepo = ordenRepo;
    }

    public Map<String, Object> motoresVsTapas(LocalDate desde, LocalDate hastaInclusive) {
        // [desde, hasta] -> [desde 00:00, (hasta+1) 00:00)
        LocalDateTime d0 = desde.atStartOfDay();
        LocalDateTime d1 = hastaInclusive.plusDays(1).atStartOfDay();

        long motores = 0, tapas = 0;
        for (var row : ordenRepo.contarPorTipoEntre(d0, d1)) {
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
}