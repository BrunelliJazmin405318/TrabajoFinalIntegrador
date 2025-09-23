package ar.edu.utn.tfi.service;
import ar.edu.utn.tfi.domain.OrdenTrabajo;
import ar.edu.utn.tfi.repository.OrdenEtapaHistorialRepository;
import ar.edu.utn.tfi.repository.OrdenTrabajoRepository;
import ar.edu.utn.tfi.web.dto.OrderStageDTO;
import ar.edu.utn.tfi.web.dto.PublicOrderDetailsDTO;
import ar.edu.utn.tfi.web.dto.PublicOrderStatusDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderQueryService {
    private final OrdenTrabajoRepository ordenRepo;
    private final OrdenEtapaHistorialRepository histRepo;

    public OrderQueryService(OrdenTrabajoRepository ordenRepo,
                             OrdenEtapaHistorialRepository histRepo) {
        this.ordenRepo = ordenRepo;
        this.histRepo = histRepo;
    }

    @Transactional(readOnly = true)
    public PublicOrderDetailsDTO getPublicDetailsByNro(String nroOrden) {
        OrdenTrabajo orden = ordenRepo.findByNroOrden(nroOrden)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada"));

        var estado = new PublicOrderStatusDTO(
                orden.getNroOrden(),
                orden.getEstadoActual(),
                orden.getGarantiaDesde(),
                orden.getGarantiaHasta(),
                orden.getCreadaEn()
        );

        var historial = histRepo.findByOrdenIdOrderByFechaInicioAsc(orden.getId())
                .stream()
                .map(h -> new OrderStageDTO(
                        h.getEtapaCodigo(),
                        h.getFechaInicio(),
                        h.getFechaFin(),
                        h.getObservacion(),
                        h.getUsuario()
                ))
                .toList();

        return new PublicOrderDetailsDTO(estado, historial);
    }

    @Transactional(readOnly = true)
    public List<OrderStageDTO> getHistorialByNro(String nroOrden) {
        var orden = ordenRepo.findByNroOrden(nroOrden)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada"));

        return histRepo.findByOrdenIdOrderByFechaInicioAsc(orden.getId())
                .stream()
                .map(h -> new OrderStageDTO(
                        h.getEtapaCodigo(),
                        h.getFechaInicio(),
                        h.getFechaFin(),
                        h.getObservacion(),
                        h.getUsuario()
                ))
                .toList();
    }
}
