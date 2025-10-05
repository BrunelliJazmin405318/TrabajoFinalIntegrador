package ar.edu.utn.tfi.web;
import ar.edu.utn.tfi.repository.OrdenTrabajoRepository;
import ar.edu.utn.tfi.service.OrderAdvanceService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/ordenes")
public class AdminOrdenController {
    private final OrdenTrabajoRepository ordenRepo;
    private final OrderAdvanceService service;

    public AdminOrdenController(OrdenTrabajoRepository ordenRepo, OrderAdvanceService service) {
        this.ordenRepo = ordenRepo;
        this.service = service;
    }

    @PostMapping("/{nro}/avanzar")
    public ResponseEntity<?> avanzarEtapaPorNro(@PathVariable String nro,
                                                Authentication auth) {
        var orden = ordenRepo.findByNroOrden(nro)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada: " + nro));
        service.avanzarEtapa(orden.getId(), auth.getName());
        return ResponseEntity.ok(Map.of("message","Etapa avanzada correctamente", "nroOrden", nro));
    }
}
