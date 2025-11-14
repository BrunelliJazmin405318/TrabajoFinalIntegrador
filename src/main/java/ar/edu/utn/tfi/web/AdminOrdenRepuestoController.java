package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.repository.OrdenRepuestoRepository;
import ar.edu.utn.tfi.repository.OrdenTrabajoRepository;
import ar.edu.utn.tfi.service.OrdenRepuestoService;
import ar.edu.utn.tfi.web.dto.RepuestoCreateReq;
import ar.edu.utn.tfi.web.dto.RepuestoDTO;
import ar.edu.utn.tfi.web.dto.RepuestoUpdateReq;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/ordenes")
public class AdminOrdenRepuestoController {

    private final OrdenRepuestoService repuestoService;
    private final OrdenRepuestoRepository ordenRepuestoRepository;
    private final OrdenTrabajoRepository ordenRepo;

    public AdminOrdenRepuestoController(OrdenRepuestoService repuestoService, OrdenRepuestoRepository ordenRepuestoRepository, OrdenTrabajoRepository ordenRepo) {
        this.repuestoService = repuestoService;
        this.ordenRepuestoRepository = ordenRepuestoRepository;
        this.ordenRepo = ordenRepo;
    }

    // Listar repuestos de una OT por Nº de orden
    @GetMapping("/{nro}/repuestos")
    public List<RepuestoDTO> listar(@PathVariable String nro) {
        return repuestoService.listarPorNro(nro);
    }

    // Agregar repuesto a una OT
    @PostMapping("/{nro}/repuestos")
    public RepuestoDTO agregar(@PathVariable String nro,
                               @RequestBody RepuestoCreateReq body,
                               Authentication auth) {
        String usuario = auth != null ? auth.getName() : "admin";
        return repuestoService.agregar(nro, body, usuario);
    }

    // Eliminar repuesto
    @DeleteMapping("/{nro}/repuestos/{id}")
    public ResponseEntity<?> eliminar(@PathVariable String nro,
                                      @PathVariable Long id,
                                      Authentication auth) {
        String usuario = auth != null ? auth.getName() : "admin";
        repuestoService.eliminar(nro, id, usuario);
        return ResponseEntity.ok(Map.of(
                "message", "Repuesto eliminado",
                "nroOrden", nro,
                "repuestoId", id
        ));
    }
    @PutMapping("/{nro}/repuestos/{repId}")
    public RepuestoDTO actualizar(
            @PathVariable String nro,
            @PathVariable Long repId,
            @RequestBody RepuestoUpdateReq body) {

        var orden = ordenRepo.findByNroOrden(nro)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada: " + nro));

        var rep = ordenRepuestoRepository.findById(repId)
                .orElseThrow(() -> new EntityNotFoundException("Repuesto no encontrado: " + repId));

        if (!rep.getOrdenId().equals(orden.getId())) {
            throw new IllegalArgumentException("El repuesto no pertenece a la orden " + nro);
        }

        // cantidad
        if (body.cantidad() != null && body.cantidad().compareTo(BigDecimal.ZERO) > 0) {
            rep.setCantidad(body.cantidad());
        }

        // precio unitario
        if (body.precioUnit() != null &&
                body.precioUnit().compareTo(BigDecimal.ZERO) >= 0) {
            rep.setPrecioUnit(body.precioUnit());
        }

        // subtotal
        rep.setSubtotal(rep.getPrecioUnit().multiply(rep.getCantidad()));

        rep = ordenRepuestoRepository.save(rep);

        return RepuestoDTO.from(rep);
    }
}