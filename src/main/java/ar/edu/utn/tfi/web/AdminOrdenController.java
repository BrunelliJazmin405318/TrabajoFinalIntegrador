package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.domain.Presupuesto;
import ar.edu.utn.tfi.domain.SolicitudPresupuesto;
import ar.edu.utn.tfi.repository.OrdenTrabajoRepository;
import ar.edu.utn.tfi.repository.PresupuestoRepository;
import ar.edu.utn.tfi.repository.SolicitudPresupuestoRepository;
import ar.edu.utn.tfi.service.CrearOrdenService;
import ar.edu.utn.tfi.service.OrderAdvanceService;
import ar.edu.utn.tfi.service.OrderIrreparableService;
import ar.edu.utn.tfi.service.OrderDelayService;
import ar.edu.utn.tfi.service.CrearOrdenService.CreateOTReq;
import ar.edu.utn.tfi.service.CrearOrdenService.CreateOTResp;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/admin/ordenes")
public class AdminOrdenController {

    private final OrdenTrabajoRepository ordenRepo;
    private final PresupuestoRepository presupuestoRepo;
    private final SolicitudPresupuestoRepository solicitudRepo;
    private final OrderAdvanceService advanceService;
    private final OrderIrreparableService irreparableService;
    private final OrderDelayService delayService;
    private final CrearOrdenService service;

    public AdminOrdenController(OrdenTrabajoRepository ordenRepo,
                                OrderAdvanceService advanceService,
                                OrderIrreparableService irreparableService,
                                OrderDelayService delayService,
                                CrearOrdenService service,
                                PresupuestoRepository presupuestoRepo,
                                SolicitudPresupuestoRepository solicitudRepo) {
        this.ordenRepo = ordenRepo;
        this.advanceService = advanceService;
        this.irreparableService = irreparableService;
        this.delayService = delayService;
        this.service = service;
        this.presupuestoRepo = presupuestoRepo;
        this.solicitudRepo = solicitudRepo;
    }

    // ---------- Avanzar etapa por NRO ----------
    @PostMapping("/{nro}/avanzar")
    public ResponseEntity<?> avanzarEtapaPorNro(@PathVariable String nro, Authentication auth) {
        var orden = ordenRepo.findByNroOrden(nro)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada: " + nro));
        advanceService.avanzarEtapa(orden.getId(), auth.getName());
        return ResponseEntity.ok(Map.of("message","Etapa avanzada correctamente", "nroOrden", nro));
    }

    // ---------- Marcar pieza irreparable ----------
    @PutMapping("/{nro}/pieza-irreparable")
    public ResponseEntity<?> marcarIrreparable(@PathVariable String nro, Authentication auth) {
        irreparableService.marcarIrreparablePorNro(nro, auth.getName());
        return ResponseEntity.ok(Map.of(
                "message", "Orden marcada como PIEZA_IRREPARABLE",
                "nroOrden", nro
        ));
    }

    // ---------- Registrar demora en etapa abierta ----------
    record DemoraReq(String motivo, String observacion) {}

    @PostMapping("/{nro}/demora")
    public ResponseEntity<?> registrarDemora(@PathVariable String nro,
                                             @RequestBody DemoraReq body,
                                             Authentication auth) {
        if (body == null || body.motivo() == null || body.motivo().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error","BAD_REQUEST",
                    "message","El campo 'motivo' es obligatorio (FALTA_REPUESTO | AUTORIZACION_CLIENTE | CAPACIDAD_TALLER)"
            ));
        }

        delayService.registrarDemora(
                nro,
                auth.getName(),
                body.motivo().trim().toUpperCase(),
                body.observacion()
        );

        return ResponseEntity.ok(Map.of("message", "Demora registrada", "nroOrden", nro));
    }

    // ---------- Crear OT genérica (si necesitás crear directo con payload) ----------
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody CrearOrdenService.CreateOTReq req,
                                   Authentication auth) {
        String usuario = (auth != null ? auth.getName() : "admin");
        var out = service.crearOT(req, usuario);
        return ResponseEntity.ok(Map.of(
                "message", "Orden creada",
                "ordenId", out.ordenId(),
                "nroOrden", out.nroOrden()
        ));
    }

    // ---------- Crear OT desde un Presupuesto APROBADO ----------
    // Toma datos de Presupuesto y COMPLEMENTA con la Solicitud (marca, modelo, nroMotor, teléfono)
    @PostMapping("/by-presupuesto/{id}")
    public CreateOTResp crearDesdePresupuesto(@PathVariable Long id, Authentication auth) {
        Presupuesto p = presupuestoRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Presupuesto no encontrado: " + id));

        // ✅ ya tenía OT: devolver la existente
        if (p.getOtNroOrden() != null && !p.getOtNroOrden().isBlank()) {
            var existente = ordenRepo.findByNroOrden(p.getOtNroOrden())
                    .orElseThrow(() -> new EntityNotFoundException("OT referenciada no existe: " + p.getOtNroOrden()));
            return new CreateOTResp(existente.getId(), existente.getNroOrden());
        }

        // ✅ NUEVO: permitir crear OT sólo con seña acreditada o pago final acreditado
        boolean senaOK  = "ACREDITADA".equalsIgnoreCase(p.getSenaEstado());
        boolean finalOK = "ACREDITADA".equalsIgnoreCase(p.getFinalEstado());
        if (!senaOK && !finalOK) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Para crear la OT primero debe estar acreditada la seña (o el pago final)."
            );
        }

        // ---- armar datos como ya lo tenías ----
        String cliNom = safe(p.getClienteNombre());
        String cliTel = null;
        String tipo   = safe(p.getPiezaTipo()); // "MOTOR" | "TAPA"
        String marca  = null, modelo = null, nro = null;

        if (p.getSolicitudId() != null) {
            var s = solicitudRepo.findById(p.getSolicitudId()).orElse(null);
            if (s != null) {
                if (isEmpty(cliNom)) cliNom = safe(s.getClienteNombre());
                cliTel = safe(s.getClienteTelefono());
                if (isEmpty(tipo))   tipo   = safe(s.getTipoUnidad());
                marca  = safe(s.getMarca());
                modelo = safe(s.getModelo());
                nro    = safe(s.getNroMotor());
            }
        }

        if (isEmpty(cliNom)) throw new IllegalArgumentException("Falta nombre del cliente");
        if (isEmpty(tipo))   throw new IllegalArgumentException("Falta tipo de unidad (MOTOR|TAPA)");

        var req = new CreateOTReq(cliNom, cliTel, tipo, marca, modelo, nro);
        String usuario = (auth != null ? auth.getName() : "admin");

        var out = service.crearOT(req, usuario);

        p.setOtNroOrden(out.nroOrden());
        presupuestoRepo.save(p);

        return out;
    }

    // ---------- Helpers ----------
    private static String safe(String s){ return s == null ? null : s.trim(); }
    private static boolean isEmpty(String s){ return s == null || s.isBlank(); }
}