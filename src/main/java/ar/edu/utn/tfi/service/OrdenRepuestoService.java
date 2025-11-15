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
import java.util.Set;

@Service
public class OrdenRepuestoService {

    private final OrdenTrabajoRepository ordenRepo;
    private final OrdenRepuestoRepository repuestoRepo;

    // 👇 ETAPAS donde SÍ se pueden tocar repuestos
    private static final Set<String> ETAPAS_PERMITEN_REPUESTOS = Set.of(
            "DESPIECE_LAVADO",
            "DIAGNOSTICO",
            "MAQUINADO",
            "SEMI_ARMADO"
    );

    public OrdenRepuestoService(OrdenTrabajoRepository ordenRepo,
                                OrdenRepuestoRepository repuestoRepo) {
        this.ordenRepo = ordenRepo;
        this.repuestoRepo = repuestoRepo;
    }
    // 🔹 Helper interno
    private void validarEtapaParaRepuestos(OrdenTrabajo orden) {
        String etapa = (orden.getEstadoActual() == null ? "" : orden.getEstadoActual().toUpperCase());
        if (!ETAPAS_PERMITEN_REPUESTOS.contains(etapa)) {
            throw new IllegalStateException(
                    "No se pueden gestionar repuestos en la etapa actual: " + etapa
            );
        }
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

        // ✅ NUEVO: validar etapa antes de tocar repuestos
        validarEtapaParaRepuestos(orden);

        OrdenRepuesto r = new OrdenRepuesto();
        r.setOrdenId(orden.getId());
        r.setDescripcion(desc);
        r.setCantidad(cantidad);
        r.setPrecioUnit(precio);
        r.setSubtotal(precio.multiply(cantidad));
        r.setCreatedBy(usuario);

        r = repuestoRepo.save(r);
        return RepuestoDTO.from(r);
    }

    @Transactional
    public void eliminar(String nroOrden, Long repuestoId, String usuario) {
        OrdenTrabajo orden = ordenRepo.findByNroOrden(nroOrden)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada: " + nroOrden));
        validarEtapaParaRepuestos(orden);

        OrdenRepuesto r = repuestoRepo.findById(repuestoId)
                .orElseThrow(() -> new EntityNotFoundException("Repuesto no encontrado: " + repuestoId));

        if (!orden.getId().equals(r.getOrdenId())) {
            throw new IllegalArgumentException("El repuesto no pertenece a la OT " + nroOrden);
        }

        repuestoRepo.delete(r);
    }

    private RepuestoDTO toDTO(OrdenRepuesto r) {
        return RepuestoDTO.from(r);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalPorNro(String nroOrden) {
        // Buscar la OT por número
        OrdenTrabajo orden = ordenRepo.findByNroOrden(nroOrden)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada: " + nroOrden));

        // Sumar precioUnit * cantidad de todos los repuestos de esa orden
        return repuestoRepo.findByOrdenIdOrderByIdAsc(orden.getId())
                .stream()
                .map(r -> {
                    BigDecimal precio = r.getPrecioUnit() != null ? r.getPrecioUnit() : BigDecimal.ZERO;
                    BigDecimal cant   = r.getCantidad() != null ? r.getCantidad() : BigDecimal.ONE;
                    return precio.multiply(cant);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    // Calcula la suma de todos los repuestos de una OT por NRO de orden
    @Transactional(readOnly = true)
    public BigDecimal calcularTotalRepuestosPorNroOrden(String nroOrden) {
        var orden = ordenRepo.findByNroOrden(nroOrden)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada: " + nroOrden));

        return repuestoRepo.findByOrdenIdOrderByIdAsc(orden.getId())
                .stream()
                .map(r -> {
                    BigDecimal cant = (r.getCantidad() == null ? BigDecimal.ONE : r.getCantidad());
                    BigDecimal precio = (r.getPrecioUnit() == null ? BigDecimal.ZERO : r.getPrecioUnit());
                    return precio.multiply(cant);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public String getEtapaActualPorNroOrden(String nroOrden) {
        OrdenTrabajo orden = ordenRepo.findByNroOrden(nroOrden)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada: " + nroOrden));
        return orden.getEstadoActual();
    }
}