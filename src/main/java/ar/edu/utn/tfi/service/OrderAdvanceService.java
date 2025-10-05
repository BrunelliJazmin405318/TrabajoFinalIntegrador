package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.domain.EtapaCatalogo;
import ar.edu.utn.tfi.domain.OrdenEtapaHistorial;
import ar.edu.utn.tfi.domain.OrdenTrabajo;
import ar.edu.utn.tfi.repository.EtapaCatalogoRepository;
import ar.edu.utn.tfi.repository.OrdenEtapaHistorialRepository;
import ar.edu.utn.tfi.repository.OrdenTrabajoRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class OrderAdvanceService {
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

        // 2) Buscar etapa actual en catálogo
        EtapaCatalogo actual = etapaRepo.findByCodigo(etapaActual)
                .orElseThrow(() -> new IllegalStateException("Etapa actual no válida: " + etapaActual));

        // 3) Buscar siguiente por 'orden + 1'
        EtapaCatalogo siguiente = etapaRepo.findByOrden(actual.getOrden() + 1)
                .orElseThrow(() -> new IllegalStateException("No hay siguiente etapa para " + etapaActual));

        // 4) Actualizar estado de orden
        orden.setEstadoActual(siguiente.getCodigo());
        ordenRepo.save(orden);

        // 5) Insertar en historial (usamos FK por id y código existente en catálogo)
        OrdenEtapaHistorial nuevoHist = new OrdenEtapaHistorial();
        nuevoHist.setOrdenId(orden.getId());
        nuevoHist.setEtapaCodigo(siguiente.getCodigo());
        nuevoHist.setFechaInicio(LocalDateTime.now());
        nuevoHist.setFechaFin(null);
        nuevoHist.setObservacion("Avance automático");
        nuevoHist.setUsuario(usuario);
        historialRepo.save(nuevoHist);

        // 6) Notificación si llega a LISTO_RETIRAR
        if ("LISTO_RETIRAR".equals(siguiente.getCodigo())) {
            System.out.println("🔔 Notificación: orden " + orden.getNroOrden() + " lista para retirar.");
        }
    }
}
