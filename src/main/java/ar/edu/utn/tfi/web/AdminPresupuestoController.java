package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.service.PresupuestoService;
import ar.edu.utn.tfi.web.dto.SolicitudDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/presupuestos")
public class AdminPresupuestoController {
    private final PresupuestoService service;

    public AdminPresupuestoController(PresupuestoService service) {
        this.service = service;
    }

    @GetMapping("/solicitudes")
    public List<SolicitudDTO> listar(@RequestParam(required = false) String estado) {
        return service.listar(estado).stream().map(SolicitudDTO::from).toList();
    }

    @PutMapping("/solicitudes/{id}/aprobar")
    public ResponseEntity<?> aprobar(@PathVariable Long id,
                                     @RequestBody(required = false) Map<String,String> body,
                                     Authentication auth) {
        String nota = body != null ? body.getOrDefault("nota","") : "";
        var s = service.aprobar(id, auth.getName(), nota);
        return ResponseEntity.ok(Map.of("message","Aprobada","id", s.getId()));
    }

    @PutMapping("/solicitudes/{id}/rechazar")
    public ResponseEntity<?> rechazar(@PathVariable Long id,
                                      @RequestBody(required = false) Map<String,String> body,
                                      Authentication auth) {
        String nota = body != null ? body.getOrDefault("nota","") : "";
        var s = service.rechazar(id, auth.getName(), nota);
        return ResponseEntity.ok(Map.of("message","Rechazada","id", s.getId()));
    }
}
