package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.domain.AuditoriaCambio;
import ar.edu.utn.tfi.domain.DemoraMotivo;
import ar.edu.utn.tfi.domain.OrdenEtapaHistorial;
import ar.edu.utn.tfi.domain.OrdenTrabajo;
import ar.edu.utn.tfi.repository.AuditoriaCambioRepository;
import ar.edu.utn.tfi.repository.DemoraMotivoRepository;
import ar.edu.utn.tfi.repository.OrdenEtapaHistorialRepository;
import ar.edu.utn.tfi.repository.OrdenTrabajoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DemoraService {

    private static final String ETAPA_PERMITIDA = "SEMI_ARMADO";

    private final OrdenTrabajoRepository ordenRepo;
    private final OrdenEtapaHistorialRepository historialRepo;
    private final DemoraMotivoRepository motivoRepo;
    private final AuditoriaCambioRepository auditoriaRepo; // 👈 NEW

    public DemoraService(OrdenTrabajoRepository ordenRepo,
                         OrdenEtapaHistorialRepository historialRepo,
                         DemoraMotivoRepository motivoRepo,
                         AuditoriaCambioRepository auditoriaRepo) {
        this.ordenRepo = ordenRepo;
        this.historialRepo = historialRepo;
        this.motivoRepo = motivoRepo;
        this.auditoriaRepo = auditoriaRepo;
    }

    /**
     * Registra una demora sobre la ETAPA ACTIVA (sin crear fila nueva).
     * - Solo en SEMI_ARMADO.
     * - Actualiza observación de la fila activa y setea motivo.
     * - Inserta auditoría directamente (sin pasar por otro servicio).
     */
    @Transactional
    public void registrarDemoraPorNro(String nroOrden, String codigoMotivo, String observacion, String usuario) {
        System.out.println("🟡 Iniciando registro de demora para orden " + nroOrden);

        OrdenTrabajo orden = ordenRepo.findByNroOrden(nroOrden)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada: " + nroOrden));

        String etapaActual = (orden.getEstadoActual() == null) ? "" : orden.getEstadoActual().toUpperCase();
        if (!ETAPA_PERMITIDA.equals(etapaActual)) {
            throw new IllegalStateException(
                    "La demora sólo puede registrarse en la etapa " + ETAPA_PERMITIDA +
                            " (actual: " + (orden.getEstadoActual() == null ? "-" : orden.getEstadoActual()) + ")"
            );
        }

        DemoraMotivo motivo = motivoRepo.findByCodigo(codigoMotivo)
                .orElseThrow(() -> new EntityNotFoundException("Motivo de demora inexistente: " + codigoMotivo));

        OrdenEtapaHistorial activa = historialRepo
                .findTopByOrdenIdAndFechaFinIsNullOrderByFechaInicioDesc(orden.getId())
                .orElseThrow(() -> new IllegalStateException("No hay etapa activa en el historial para esta orden."));

        String anteriorObs = nullToEmpty(activa.getObservacion());
        String nuevaObs = buildObservacionConDemora(anteriorObs, motivo.getCodigo(), observacion);

        // Actualizar fila activa (NO se crea nueva)
        activa.setObservacion(nuevaObs);
        activa.setDemoraMotivoId(motivo.getId());
        historialRepo.save(activa);

        System.out.println("✅ Demora aplicada a historial. Grabando auditoría…");

        // 👇 Auditar acá mismo, directo al repo (saveAndFlush para ver el INSERT al toque)
        AuditoriaCambio a = new AuditoriaCambio();
        a.setOrdenId(orden.getId());
        a.setCampo("demora");
        a.setValorAnterior(emptyToNull(anteriorObs));
        a.setValorNuevo(emptyToNull(nuevaObs));
        a.setUsuario(usuario);
        a.setFecha(LocalDateTime.now());
        auditoriaRepo.saveAndFlush(a);

        System.out.println("✅ Auditoría INSERTADA para ordenId=" + orden.getId());
    }

    private String buildObservacionConDemora(String anterior, String codigoMotivo, String observacionLibre) {
        String base = (anterior == null || anterior.isBlank()) ? "" : anterior.trim();
        String sufix = "DEMORA: " + codigoMotivo;
        if (observacionLibre != null && !observacionLibre.isBlank()) {
            sufix = sufix + " - " + observacionLibre.trim();
        }

        if (base.isEmpty()) {
            return sufix;
        }
        if (base.endsWith("|")) {
            return base + " " + sufix;
        }
        return base + " | " + sufix;
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}