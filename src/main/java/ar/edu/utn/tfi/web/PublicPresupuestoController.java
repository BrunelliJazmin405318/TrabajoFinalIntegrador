package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.domain.Presupuesto;
import ar.edu.utn.tfi.domain.SolicitudPresupuesto;
import ar.edu.utn.tfi.repository.PresupuestoRepository;
import ar.edu.utn.tfi.service.PresupuestoService;
import ar.edu.utn.tfi.web.dto.SolicitudCreateDTO;
import ar.edu.utn.tfi.web.dto.SolicitudDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ar.edu.utn.tfi.security.PinService;
import ar.edu.utn.tfi.security.PublicAuth;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/public/presupuestos")
public class PublicPresupuestoController {

    private final PresupuestoService service;
    private final PinService pinService;

    public PublicPresupuestoController(PresupuestoService service, PinService pinService) {
        this.service = service;
        this.pinService = pinService;
    }

    // Regex súper simple para email (demo)
    private static final Pattern EMAIL_RE =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private static String s(String v) {
        return v == null ? "" : v.trim();
    }

    // Crear solicitud (público) CON VALIDACIONES
    @PostMapping("/solicitud")
    public ResponseEntity<?> crear(@RequestBody SolicitudCreateDTO dto) {

        Map<String, String> errors = new LinkedHashMap<>();

        // Normalizo todo
        String nombre       = s(dto.clienteNombre());
        String tel          = s(dto.clienteTelefono());
        String email        = s(dto.clienteEmail());
        String tipoUnidad   = s(dto.tipoUnidad()).toUpperCase();
        String marca        = s(dto.marca());
        String modelo       = s(dto.modelo());
        String motor        = s(dto.nroMotor());
        String desc         = s(dto.descripcion());
        String tipoConsulta = s(dto.tipoConsulta()).toUpperCase();  // ⬅️ NUEVO

        // ===== tipoConsulta: default & validación =====
        if (tipoConsulta.isBlank()) {
            tipoConsulta = "COTIZACION";  // valor por defecto
        } else if (!tipoConsulta.equals("COTIZACION") && !tipoConsulta.equals("DIAGNOSTICO")) {
            errors.put("tipoConsulta", "El tipo de consulta debe ser COTIZACION o DIAGNOSTICO.");
        }
        boolean esDiagnostico = "DIAGNOSTICO".equals(tipoConsulta);

        // ===== Validaciones campo por campo =====

        if (nombre.isBlank()) {
            errors.put("clienteNombre", "El nombre es obligatorio.");
        } else if (nombre.length() > 80) {
            errors.put("clienteNombre", "El nombre no puede superar los 80 caracteres.");
        }

        if (tel.isBlank()) {
            errors.put("clienteTelefono", "El teléfono es obligatorio.");
        } else if (tel.length() > 30) {
            errors.put("clienteTelefono", "El teléfono no puede superar los 30 caracteres.");
        }

        // Email opcional (porque notifican por WhatsApp)
        if (!email.isBlank() && !EMAIL_RE.matcher(email).matches()) {
            errors.put("clienteEmail", "El email no tiene un formato válido.");
        }

        if (tipoUnidad.isBlank()) {
            errors.put("tipoUnidad", "El tipo de unidad es obligatorio.");
        } else if (!tipoUnidad.equals("MOTOR") && !tipoUnidad.equals("TAPA")) {
            errors.put("tipoUnidad", "El tipo de unidad debe ser MOTOR o TAPA.");
        }

        // 🔴 Diferen­cia clave:
        //  - COTIZACION → marca/modelo/motor OBLIGATORIOS (como antes)
        //  - DIAGNOSTICO → solo validamos longitudes si los completa

        if (!esDiagnostico) {
            // === flujo COTIZACION: obligatorio ===
            if (marca.isBlank()) {
                errors.put("marca", "La marca es obligatoria.");
            } else if (marca.length() > 50) {
                errors.put("marca", "La marca no puede superar los 50 caracteres.");
            }

            if (modelo.isBlank()) {
                errors.put("modelo", "El modelo es obligatorio.");
            } else if (modelo.length() > 50) {
                errors.put("modelo", "El modelo no puede superar los 50 caracteres.");
            }

            if (motor.isBlank()) {
                errors.put("nroMotor", "El número de motor es obligatorio.");
            } else if (motor.length() > 50) {
                errors.put("nroMotor", "El número de motor no puede superar los 50 caracteres.");
            }
        } else {
            // === flujo DIAGNOSTICO: opcionales, solo límites de longitud ===
            if (!marca.isBlank() && marca.length() > 50) {
                errors.put("marca", "La marca no puede superar los 50 caracteres.");
            }
            if (!modelo.isBlank() && modelo.length() > 50) {
                errors.put("modelo", "El modelo no puede superar los 50 caracteres.");
            }
            if (!motor.isBlank() && motor.length() > 50) {
                errors.put("nroMotor", "El número de motor no puede superar los 50 caracteres.");
            }
        }

        if (desc.isBlank()) {
            errors.put("descripcion", "La descripción del trabajo es obligatoria.");
        } else if (desc.length() > 1000) {
            errors.put("descripcion", "La descripción no puede superar los 1000 caracteres.");
        }

        // Si hay errores, devolvemos 400 con el detalle
        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "VALIDATION_ERROR",
                    "errors", errors
            ));
        }

        // Creamos un DTO "limpio" con los valores normalizados
        SolicitudCreateDTO cleanDto = new SolicitudCreateDTO(
                nombre,
                tel,
                email,
                tipoUnidad,   // MOTOR | TAPA en mayúsculas
                marca,
                modelo,
                motor,
                desc,
                tipoConsulta  // ⬅️ NUEVO
        );

        String pin = pinService.generarPin6();

        var s = service.crearSolicitud(cleanDto, pin);

        return ResponseEntity.ok(Map.of(
                "id", s.getId(),
                "estado", s.getEstado()
        ));
    }

    @GetMapping("/solicitud/{id}")
    public ResponseEntity<?> ver(@PathVariable Long id) {

        Long sid = PublicAuth.solicitudIdFromTokenOrNull();
        if (sid == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "TOKEN_REQUIRED",
                    "message", "Falta el token. Iniciá con PIN para obtener accessToken."
            ));
        }

        if (!sid.equals(id)) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "FORBIDDEN",
                    "message", "El token no corresponde a esta solicitud."
            ));
        }

        SolicitudPresupuesto s = service.getById(id);

        List<Presupuesto> aprobados = service.listar("APROBADO", id);
        Presupuesto p = !aprobados.isEmpty()
                ? aprobados.get(0)
                : firstOrNull(service.listar(null, id));

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
        out.put("tipoConsulta", s.getTipoConsulta());
        out.put("estado", s.getEstado());
        out.put("creadaEn", s.getCreadaEn());
        out.put("decisionUsuario", s.getDecisionUsuario());
        out.put("decisionFecha", s.getDecisionFecha());
        out.put("decisionMotivo", s.getDecisionMotivo());

        if (p != null) {
            out.put("presupuestoId", p.getId());
            out.put("presupuestoEstado", p.getEstado());
            out.put("total", p.getTotal());
            out.put("senaEstado", p.getSenaEstado());
            out.put("senaMonto", p.getSenaMonto());
            out.put("senaPaymentId", p.getSenaPaymentId());
            out.put("senaPaymentStatus", p.getSenaPaymentStatus());
            out.put("senaPaidAt", p.getSenaPaidAt());
            out.put("otNroOrden", p.getOtNroOrden());
        }

        return ResponseEntity.ok(out);
    }
    private static Presupuesto firstOrNull(List<Presupuesto> list) {
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }
}