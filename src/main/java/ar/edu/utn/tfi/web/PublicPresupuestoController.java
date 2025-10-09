package ar.edu.utn.tfi.web;
import ar.edu.utn.tfi.service.PresupuestoService;
import ar.edu.utn.tfi.web.dto.SolicitudCreateDTO;
import ar.edu.utn.tfi.web.dto.SolicitudDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/public/presupuestos")
public class PublicPresupuestoController {
    private final PresupuestoService service;

    public PublicPresupuestoController(PresupuestoService service) {
        this.service = service;
    }

    @PostMapping("/solicitud")
    public ResponseEntity<?> crear(@RequestBody SolicitudCreateDTO dto) {
        var s = service.crearSolicitud(dto);
        return ResponseEntity.ok(Map.of(
                "id", s.getId(),
                "estado", s.getEstado()
        ));
    }

    @GetMapping("/solicitud/{id}")
    public SolicitudDTO ver(@PathVariable Long id) {
        return SolicitudDTO.from(service.getById(id));
    }
}
