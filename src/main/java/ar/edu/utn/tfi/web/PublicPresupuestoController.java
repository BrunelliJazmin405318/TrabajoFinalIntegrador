package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.domain.Presupuesto;
import ar.edu.utn.tfi.domain.SolicitudPresupuesto;
import ar.edu.utn.tfi.service.PresupuestoService;
import ar.edu.utn.tfi.web.dto.SolicitudCreateDTO;
import ar.edu.utn.tfi.web.dto.SolicitudDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/public/presupuestos")
public class PublicPresupuestoController {

    private final PresupuestoService service;

    public PublicPresupuestoController(PresupuestoService service) {
        this.service = service;
    }

    // Crear solicitud (público)
    @PostMapping("/solicitud")
    public ResponseEntity<?> crear(@RequestBody SolicitudCreateDTO dto) {
        var s = service.crearSolicitud(dto);
        return ResponseEntity.ok(Map.of(
                "id", s.getId(),
                "estado", s.getEstado()
        ));
    }

    // Ver solicitud (público) — devuelve también info del presupuesto asociado si existe
    @GetMapping("/solicitud/{id}")
    public ResponseEntity<?> ver(@PathVariable Long id) {
        SolicitudPresupuesto s = service.getById(id); // lanza EntityNotFound si no existe

        // Intentamos traer el presupuesto más reciente; si hay uno APROBADO, priorizarlo
        List<Presupuesto> aprobados = service.listar("APROBADO", id);
        Presupuesto p = !aprobados.isEmpty()
                ? aprobados.get(0)
                : firstOrNull(service.listar(null, id)); // cualquier estado, el más nuevo

        // Armamos respuesta base con datos de la solicitud
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", s.getId());
        out.put("clienteNombre", s.getClienteNombre());
        out.put("clienteTelefono", s.getClienteTelefono());
        out.put("clienteEmail", s.getClienteEmail());
        out.put("tipoUnidad", s.getTipoUnidad());
        out.put("marca", s.getMarca());
        out.put("modelo", s.getModelo());
        out.put("nroMotor", s.getNroMotor());
        out.put("descripcion", s.getDescripcion());
        out.put("estado", s.getEstado());
        out.put("creadaEn", s.getCreadaEn());
        out.put("decisionUsuario", s.getDecisionUsuario());
        out.put("decisionFecha", s.getDecisionFecha());
        out.put("decisionMotivo", s.getDecisionMotivo());

        // Si hay presupuesto, agregamos info para el botón de pago en estado-solicitud.html
        if (p != null) {
            out.put("presupuestoId", p.getId());
            out.put("presupuestoEstado", p.getEstado());
            out.put("total", p.getTotal());
            out.put("senaEstado", p.getSenaEstado());
            out.put("senaMonto", p.getSenaMonto());
            out.put("senaPaymentId", p.getSenaPaymentId());
            out.put("senaPaymentStatus", p.getSenaPaymentStatus());
            out.put("senaPaidAt", p.getSenaPaidAt());
        }

        return ResponseEntity.ok(out);
    }

    private static Presupuesto firstOrNull(List<Presupuesto> list) {
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }
}