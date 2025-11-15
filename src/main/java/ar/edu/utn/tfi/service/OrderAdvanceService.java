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
import java.time.ZoneOffset;

@Service
public class OrderAdvanceService {
    private final OrdenTrabajoRepository ordenRepo;
    private final OrdenEtapaHistorialRepository historialRepo;
    private final EtapaCatalogoRepository etapaRepo;
    private final AuditoriaService auditoria;
    private final NotificationService notificationService;

    public OrderAdvanceService(OrdenTrabajoRepository ordenRepo,
                               OrdenEtapaHistorialRepository historialRepo,
                               EtapaCatalogoRepository etapaRepo,
                               AuditoriaService auditoria,
                               NotificationService notificationService) {
        this.ordenRepo = ordenRepo;
        this.historialRepo = historialRepo;
        this.etapaRepo = etapaRepo;
        this.auditoria = auditoria;
        this.notificationService = notificationService;
    }

    // ✅ Agregá este helper acá mismo:
    private LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
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

        // 4) Cerrar la etapa previa abierta (si existe)
        historialRepo.findTopByOrdenIdAndFechaFinIsNullOrderByFechaInicioDesc(orden.getId())
                .ifPresent(h -> {
                    h.setFechaFin(nowUtc());
                    historialRepo.save(h);
                });

        // 5) Actualizar estado de la OT
        String nuevoEstado = siguiente.getCodigo();
        if (nuevoEstado.equalsIgnoreCase(etapaActual)) return;

        orden.setEstadoActual(nuevoEstado);
        ordenRepo.save(orden);

        // 6) Garantía automática al ENTREGADO
        if ("ENTREGADO".equalsIgnoreCase(nuevoEstado)) {
            orden.setGarantiaDesde(java.time.LocalDate.now());
            orden.setGarantiaHasta(java.time.LocalDate.now().plusDays(90));
            ordenRepo.save(orden);

            auditoria.registrarCambio(orden.getId(), "garantia_desde", null,
                    orden.getGarantiaDesde().toString(), usuario);
            auditoria.registrarCambio(orden.getId(), "garantia_hasta", null,
                    orden.getGarantiaHasta().toString(), usuario);

            System.out.println("🧾 Garantía registrada: desde " + orden.getGarantiaDesde() +
                    " hasta " + orden.getGarantiaHasta());
        }

        // 7) Insertar nueva fila en historial (apertura de la nueva etapa)
        OrdenEtapaHistorial nuevoHist = new OrdenEtapaHistorial();
        nuevoHist.setOrdenId(orden.getId());
        nuevoHist.setEtapaCodigo(nuevoEstado);
        nuevoHist.setFechaInicio(nowUtc());
        nuevoHist.setObservacion("Avance automático");
        nuevoHist.setUsuario(usuario);
        historialRepo.save(nuevoHist);

        // 8) Auditoría cambio de estado
        auditoria.registrarCambio(
                orden.getId(),
                "estado_actual",
                etapaActual,
                nuevoEstado,
                usuario
        );
        // 7) Notificación mock
        //if ("LISTO_RETIRAR".equalsIgnoreCase(nuevoEstado)) {
        //     System.out.println("🔔 Notificación: orden " + orden.getNroOrden() + " lista para retirar.");
        // }
        if ("LISTO_RETIRAR".equalsIgnoreCase(nuevoEstado)) {
            try {
                notificationService.emitirListoRetirar(orden, null);
            } catch (Exception e) {
                // Por ahora no rompemos el avance de etapa si falla la notificación
                System.err.println("⚠ No se pudo emitir notificación LISTO_RETIRAR: " + e.getMessage());
            }
        }
    }
}