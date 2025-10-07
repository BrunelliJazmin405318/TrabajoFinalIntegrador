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

    private static final String ETAPA_IRREPARABLE = "PIEZA_IRREPARABLE";
    private static final String ETAPA_ENTREGADO = "ENTREGADO";

    private final OrdenTrabajoRepository ordenRepo;
    private final OrdenEtapaHistorialRepository historialRepo;
    private final EtapaCatalogoRepository etapaRepo;

    public OrderAdvanceService(OrdenTrabajoRepository ordenRepo,
                               OrdenEtapaHistorialRepository historialRepo,
                               EtapaCatalogoRepository etapaRepo) {
        this.ordenRepo = ordenRepo;
        this.historialRepo = historialRepo;
        this.etapaRepo = etapaRepo;
    }

    @Transactional
    public void avanzarEtapa(Long id, String usuario) {
        // 1) Traer orden
        OrdenTrabajo orden = ordenRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada"));

        String etapaActual = orden.getEstadoActual();
        if (etapaActual == null || etapaActual.isBlank()) {
            throw new IllegalStateException("La orden no tiene etapa actual definida");
        }

        // 1.1) Bloquear si es un estado terminal o especial
        String actualUpper = etapaActual.toUpperCase();
        if (ETAPA_IRREPARABLE.equals(actualUpper)) {
            throw new IllegalStateException("No se puede avanzar: la pieza fue marcada como irreparable");
        }
        if (ETAPA_ENTREGADO.equals(actualUpper)) {
            throw new IllegalStateException("No se puede avanzar: la orden ya fue ENTREGADO");
        }

        // 2) Buscar etapa actual en catálogo
        EtapaCatalogo actual = etapaRepo.findByCodigo(etapaActual)
                .orElseThrow(() -> new IllegalStateException("Etapa actual no válida: " + etapaActual));

        // 3) Buscar siguiente por 'orden + 1'
        EtapaCatalogo siguiente = etapaRepo.findByOrden(actual.getOrden() + 1)
                .orElseThrow(() -> new IllegalStateException("No hay siguiente etapa para " + etapaActual));

        // 4) Cerrar la etapa abierta (si existe) con UTC y respetando constraint fecha_fin >= fecha_inicio
        historialRepo.findTopByOrdenIdAndFechaFinIsNullOrderByFechaInicioDesc(orden.getId())
                .ifPresent(abierta -> {
                    LocalDateTime now = nowUtc();
                    LocalDateTime fin = now.isBefore(abierta.getFechaInicio()) ? abierta.getFechaInicio() : now;
                    abierta.setFechaFin(fin);
                    historialRepo.save(abierta);
                });

        // 5) Actualizar estado de orden
        orden.setEstadoActual(siguiente.getCodigo());
        ordenRepo.save(orden);

        // 6) Insertar nueva etapa (UTC)
        OrdenEtapaHistorial nuevoHist = new OrdenEtapaHistorial();
        nuevoHist.setOrdenId(orden.getId());
        nuevoHist.setEtapaCodigo(siguiente.getCodigo());
        nuevoHist.setFechaInicio(nowUtc());
        nuevoHist.setFechaFin(null);
        nuevoHist.setObservacion("Avance automático");
        nuevoHist.setUsuario(usuario);
        historialRepo.save(nuevoHist);

        // 7) Notificación si llega a LISTO_RETIRAR
        if ("LISTO_RETIRAR".equals(siguiente.getCodigo())) {
            System.out.println("🔔 Notificación: orden " + orden.getNroOrden() + " lista para retirar.");
        }
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}