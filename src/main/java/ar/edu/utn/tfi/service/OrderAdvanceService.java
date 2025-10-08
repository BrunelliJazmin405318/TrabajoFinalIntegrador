package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.domain.EtapaCatalogo;
import ar.edu.utn.tfi.domain.OrdenEtapaHistorial;
import ar.edu.utn.tfi.domain.OrdenTrabajo;
import ar.edu.utn.tfi.repository.EtapaCatalogoRepository;
import ar.edu.utn.tfi.repository.OrdenEtapaHistorialRepository;
import ar.edu.utn.tfi.repository.OrdenTrabajoRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OrderAdvanceService {
    private final OrdenTrabajoRepository ordenRepo;
    private final OrdenEtapaHistorialRepository historialRepo;
    private final EtapaCatalogoRepository etapaRepo;
    private final AuditoriaService auditoria;

    public OrderAdvanceService(OrdenTrabajoRepository ordenRepo,
                               OrdenEtapaHistorialRepository historialRepo,
                               EtapaCatalogoRepository etapaRepo,
                               AuditoriaService auditoria) {
        this.ordenRepo = ordenRepo;
        this.historialRepo = historialRepo;
        this.etapaRepo = etapaRepo;
        this.auditoria = auditoria;
    }

    @Transactional
    public void avanzarEtapa(Long id, String usuario) {
        // 1) Traer orden
        OrdenTrabajo orden = ordenRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada"));

        String etapaActual = orden.getEstadoActual();

        // 2) Validar etapa actual en catálogo
        EtapaCatalogo actual = etapaRepo.findByCodigo(etapaActual)
                .orElseThrow(() -> new IllegalStateException("Etapa actual no válida: " + etapaActual));

        // 3) Buscar siguiente (orden + 1)
        EtapaCatalogo siguiente = etapaRepo.findByOrden(actual.getOrden() + 1)
                .orElseThrow(() -> new IllegalStateException("No hay siguiente etapa para " + etapaActual));

        // 4) Actualizar estado de la OT
        String nuevoEstado = siguiente.getCodigo();
        if (nuevoEstado.equals(etapaActual)) {
            // No hay cambio; evitamos ruido de auditoría e historial
            return;
        }
        orden.setEstadoActual(nuevoEstado);
        ordenRepo.save(orden);

        // 5) Insertar nueva fila en historial (apertura de la etapa siguiente)
        OrdenEtapaHistorial nuevoHist = new OrdenEtapaHistorial();
        nuevoHist.setOrdenId(orden.getId());
        nuevoHist.setEtapaCodigo(nuevoEstado);
        nuevoHist.setFechaInicio(LocalDateTime.now());
        nuevoHist.setFechaFin(null);
        nuevoHist.setObservacion("Avance automático");
        nuevoHist.setUsuario(usuario);
        historialRepo.save(nuevoHist);

        // 6) AUDITORÍA DE CAMBIO (estado_actual)
        auditoria.registrarCambio(
                orden.getId(),
                "estado_actual",
                etapaActual,
                nuevoEstado,
                usuario
        );

        // 7) Notificación (mock) si llega a LISTO_RETIRAR
        if ("LISTO_RETIRAR".equals(nuevoEstado)) {
            System.out.println("🔔 Notificación: orden " + orden.getNroOrden() + " lista para retirar.");
        }
    }
}