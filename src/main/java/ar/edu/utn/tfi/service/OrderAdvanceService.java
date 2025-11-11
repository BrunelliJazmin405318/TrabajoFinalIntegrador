// src/main/java/ar/edu/utn/tfi/service/OrderAdvanceService.java
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
    private final NotificacionService notificacionService; // ⬅️ NUEVO

    public OrderAdvanceService(OrdenTrabajoRepository ordenRepo,
                               OrdenEtapaHistorialRepository historialRepo,
                               EtapaCatalogoRepository etapaRepo,
                               AuditoriaService auditoria,
                               NotificacionService notificacionService) { // ⬅️ NUEVO
        this.ordenRepo = ordenRepo;
        this.historialRepo = historialRepo;
        this.etapaRepo = etapaRepo;
        this.auditoria = auditoria;
        this.notificacionService = notificacionService; // ⬅️ NUEVO
    }

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

        // 4) Actualizar estado de la OT
        String nuevoEstado = siguiente.getCodigo();
        if (nuevoEstado.equals(etapaActual)) return;

        orden.setEstadoActual(nuevoEstado);
        ordenRepo.save(orden);

        // ✅ Si llega a ENTREGADO, registrar garantía automática
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

        // 5) Insertar nueva fila en historial
        OrdenEtapaHistorial nuevoHist = new OrdenEtapaHistorial();
        nuevoHist.setOrdenId(orden.getId());
        nuevoHist.setEtapaCodigo(nuevoEstado);
        nuevoHist.setFechaInicio(nowUtc());
        nuevoHist.setObservacion("Avance automático");
        nuevoHist.setUsuario(usuario);
        historialRepo.save(nuevoHist);

        // 6) Auditoría cambio de estado
        auditoria.registrarCambio(
                orden.getId(),
                "estado_actual",
                etapaActual,
                nuevoEstado,
                usuario
        );

        // 7) 🚀 Disparar notificación cuando queda LISTO_RETIRAR
        if ("LISTO_RETIRAR".equalsIgnoreCase(nuevoEstado)) {
            // Centralizamos todo en el servicio de notificaciones
            try {
                notificacionService.registrarMotorListo(
                        orden.getId(),
                        orden.getClienteEmail(),
                        orden.getNroOrden()
                );
            } catch (Exception ex) {
                // No frenamos el avance si falla el aviso
                System.out.println("⚠️ Error registrando notificación LISTO_RETIRAR: " + ex.getMessage());
            }
            System.out.println("🔔 Notificación: orden " + orden.getNroOrden() + " lista para retirar.");
        }
    }
}
