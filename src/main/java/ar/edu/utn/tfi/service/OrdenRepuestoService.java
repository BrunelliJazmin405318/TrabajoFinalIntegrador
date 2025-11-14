package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.domain.OrdenRepuesto;
import ar.edu.utn.tfi.domain.OrdenTrabajo;
import ar.edu.utn.tfi.repository.OrdenRepuestoRepository;
import ar.edu.utn.tfi.repository.OrdenTrabajoRepository;
import ar.edu.utn.tfi.web.dto.RepuestoCreateReq;
import ar.edu.utn.tfi.web.dto.RepuestoDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrdenRepuestoService {

    private final OrdenTrabajoRepository ordenRepo;
    private final OrdenRepuestoRepository repuestoRepo;

    public OrdenRepuestoService(OrdenTrabajoRepository ordenRepo,
                                OrdenRepuestoRepository repuestoRepo) {
        this.ordenRepo = ordenRepo;
        this.repuestoRepo = repuestoRepo;
    }

    @Transactional(readOnly = true)
    public List<RepuestoDTO> listarPorNro(String nroOrden) {
        OrdenTrabajo orden = ordenRepo.findByNroOrden(nroOrden)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada: " + nroOrden));

        return repuestoRepo.findByOrdenIdOrderByIdAsc(orden.getId())
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public RepuestoDTO agregar(String nroOrden, RepuestoCreateReq req, String usuario) {
        if (req == null) throw new IllegalArgumentException("Body requerido");

        String desc = (req.descripcion() == null ? "" : req.descripcion().trim());
        if (desc.isEmpty()) throw new IllegalArgumentException("Descripción de repuesto obligatoria");

        BigDecimal cantidad = req.cantidad() == null ? BigDecimal.ONE : req.cantidad();
        if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Cantidad debe ser mayor a 0");
        }

        BigDecimal precio = req.precioUnit();
        if (precio == null || precio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Precio unitario debe ser mayor a 0");
        }

        OrdenTrabajo orden = ordenRepo.findByNroOrden(nroOrden)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada: " + nroOrden));

        OrdenRepuesto r = new OrdenRepuesto();
        r.setOrdenId(orden.getId());
        r.setDescripcion(desc);
        r.setCantidad(cantidad);
        r.setPrecioUnit(precio);
        r.setSubtotal(precio.multiply(cantidad));  // ⬅️ calcular subtotal
        r.setCreatedBy(usuario);

        r = repuestoRepo.save(r);
        return RepuestoDTO.from(r);
    }

    @Transactional
    public void eliminar(String nroOrden, Long repuestoId, String usuario) {
        OrdenTrabajo orden = ordenRepo.findByNroOrden(nroOrden)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada: " + nroOrden));

        OrdenRepuesto r = repuestoRepo.findById(repuestoId)
                .orElseThrow(() -> new EntityNotFoundException("Repuesto no encontrado: " + repuestoId));

        if (!orden.getId().equals(r.getOrdenId())) {
            throw new IllegalArgumentException("El repuesto no pertenece a la OT " + nroOrden);
        }

        repuestoRepo.delete(r);
        // Si querés, acá podrías meter auditoría de cambios de repuestos.
    }

    private RepuestoDTO toDTO(OrdenRepuesto r) {
        return RepuestoDTO.from(r);
    }
}