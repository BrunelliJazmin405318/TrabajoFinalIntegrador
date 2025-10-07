package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.domain.OrdenEtapaHistorial;
import ar.edu.utn.tfi.domain.OrdenTrabajo;
import ar.edu.utn.tfi.repository.OrdenEtapaHistorialRepository;
import ar.edu.utn.tfi.repository.OrdenTrabajoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class OrderIrreparableService {
    private static final String ETAPA_DIAGNOSTICO = "DIAGNOSTICO";
    private static final String ETAPA_IRREPARABLE = "PIEZA_IRREPARABLE";

    private final OrdenTrabajoRepository ordenRepo;
    private final OrdenEtapaHistorialRepository historialRepo;

    public OrderIrreparableService(OrdenTrabajoRepository ordenRepo,
                                   OrdenEtapaHistorialRepository historialRepo) {
        this.ordenRepo = ordenRepo;
        this.historialRepo = historialRepo;
    }

    @Transactional
    public void marcarIrreparablePorNro(String nroOrden, String usuario) {
        OrdenTrabajo orden = ordenRepo.findByNroOrden(nroOrden)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada: " + nroOrden));

        String actual = (orden.getEstadoActual() == null) ? "" : orden.getEstadoActual().toUpperCase();
        if (!ETAPA_DIAGNOSTICO.equals(actual)) {
            throw new IllegalStateException("Sólo puede marcarse irreparable desde DIAGNOSTICO (actual: " + actual + ")");
        }

        // 1) cerrar la etapa abierta (si existe) respetando el CHECK
        historialRepo.findTopByOrdenIdAndFechaFinIsNullOrderByFechaInicioDesc(orden.getId())
                .ifPresent(abierta -> {
                    LocalDateTime now = nowUtc(); // UTC
                    LocalDateTime fin = now.isBefore(abierta.getFechaInicio()) ? abierta.getFechaInicio() : now;
                    abierta.setFechaFin(fin);
                    historialRepo.save(abierta);
                });

        // 2) cambiar estado
        orden.setEstadoActual(ETAPA_IRREPARABLE);

        // 3) insertar nueva etapa
        OrdenEtapaHistorial h = new OrdenEtapaHistorial();
        h.setOrdenId(orden.getId());
        h.setEtapaCodigo(ETAPA_IRREPARABLE);
        h.setFechaInicio(nowUtc()); // UTC
        h.setObservacion("Pieza declarada irreparable");
        h.setUsuario(usuario);

        historialRepo.save(h);
        ordenRepo.save(orden);
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}