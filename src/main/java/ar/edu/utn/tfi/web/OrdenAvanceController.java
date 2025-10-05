package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.repository.OrdenTrabajoRepository;
import ar.edu.utn.tfi.service.OrderAdvanceService;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/orden")
public class OrdenAvanceController {
    private final OrderAdvanceService service;
    private final OrdenTrabajoRepository ordenRepo;

    public OrdenAvanceController(OrderAdvanceService service, OrdenTrabajoRepository ordenRepo) {
        this.service = service;
        this.ordenRepo = ordenRepo;
    }


    @PostMapping("/{id}/avanzar")
    public ResponseEntity<?> avanzarEtapaPorId(@PathVariable Long id,
                                               Authentication auth) {
        service.avanzarEtapa(id, auth.getName());
        return ResponseEntity.ok(Map.of("message","Etapa avanzada correctamente"));
    }

}
