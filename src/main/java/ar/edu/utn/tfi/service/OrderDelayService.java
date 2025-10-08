package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.domain.OrdenEtapaHistorial;
import ar.edu.utn.tfi.domain.OrdenTrabajo;
import ar.edu.utn.tfi.repository.OrdenEtapaHistorialRepository;
import ar.edu.utn.tfi.repository.OrdenTrabajoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderDelayService {

    private static final String ETAPA_SEMI_ARMADO = "SEMI_ARMADO";

    private final OrdenTrabajoRepository ordenRepo;
    private final OrdenEtapaHistorialRepository historialRepo;

    public OrderDelayService(OrdenTrabajoRepository ordenRepo,
                             OrdenEtapaHistorialRepository historialRepo) {
        this.ordenRepo = ordenRepo;
        this.historialRepo = historialRepo;
    }

    /**
     * Registra una demora SIN crear una nueva fila:
     * - Sólo permitido si la orden está en SEMI_ARMADO.
     * - Actualiza la observación de la fila ABIERTA (fecha_fin IS NULL) de esa misma etapa.
     */
    @Transactional
    public void registrarDemora(String nroOrden, String usuario, String motivo, String observacion) {
        OrdenTrabajo orden = ordenRepo.findByNroOrden(nroOrden)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada: " + nroOrden));

        String actual = (orden.getEstadoActual() == null) ? "" : orden.getEstadoActual().toUpperCase();
        if (!ETAPA_SEMI_ARMADO.equals(actual)) {
            throw new IllegalStateException("La demora sólo puede registrarse en SEMI_ARMADO (actual: " + actual + ")");
        }

        // Buscar la fila abierta de historial (misma orden)
        OrdenEtapaHistorial abierta = historialRepo
                .findTopByOrdenIdAndFechaFinIsNullOrderByFechaInicioDesc(orden.getId())
                .orElseThrow(() -> new IllegalStateException("No hay una etapa abierta para actualizar"));

        // Asegurarnos de que esa fila abierta corresponda a SEMI_ARMADO
        if (!ETAPA_SEMI_ARMADO.equalsIgnoreCase(abierta.getEtapaCodigo())) {
            throw new IllegalStateException("La etapa abierta no es SEMI_ARMADO, no se puede registrar demora aquí.");
        }

        // Armar texto de demora y concatenarlo a la observación existente
        String base = (abierta.getObservacion() == null || abierta.getObservacion().isBlank())
                ? ""
                : abierta.getObservacion().trim();

        String demoraTxt = "DEMORA: " + motivo.trim();
        if (observacion != null && !observacion.isBlank()) {
            demoraTxt += " — " + observacion.trim();
        }

        String nuevaObs = base.isBlank() ? demoraTxt : (base + " | " + demoraTxt);
        abierta.setObservacion(nuevaObs);
        // (opcional) registrar quién hizo el cambio
        abierta.setUsuario(usuario);

        historialRepo.save(abierta);
    }
}