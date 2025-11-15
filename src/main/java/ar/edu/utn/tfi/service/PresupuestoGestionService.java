package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.domain.PagoManual;
import ar.edu.utn.tfi.domain.Presupuesto;
import ar.edu.utn.tfi.domain.PresupuestoItem;
import ar.edu.utn.tfi.domain.ServicioTarifa;
import ar.edu.utn.tfi.domain.SolicitudPresupuesto;
import ar.edu.utn.tfi.repository.PagoManualRepository;
import ar.edu.utn.tfi.repository.PresupuestoItemRepository;
import ar.edu.utn.tfi.repository.PresupuestoRepository;
import ar.edu.utn.tfi.repository.ServicioTarifaRepository;
import ar.edu.utn.tfi.repository.SolicitudPresupuestoRepository;
import ar.edu.utn.tfi.service.Pagos.PaymentApiService;
import ar.edu.utn.tfi.web.dto.ExtraItemReq;
import ar.edu.utn.tfi.web.dto.PagoApiReq;
import ar.edu.utn.tfi.web.dto.PagoInfoDTO;
import ar.edu.utn.tfi.web.dto.PagoManualReq;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PresupuestoGestionService {

    private final SolicitudPresupuestoRepository solicitudRepo;
    private final ServicioTarifaRepository tarifaRepo;
    private final PresupuestoRepository presupuestoRepo;
    private final PresupuestoItemRepository itemRepo;
    private final MailService mailService;
    private final PaymentApiService paymentApiService;
    private final PagoManualRepository pagoManualRepo;
    private final OrdenRepuestoService ordenRepuestoService;

    public PresupuestoGestionService(SolicitudPresupuestoRepository solicitudRepo,
                                     ServicioTarifaRepository tarifaRepo,
                                     PresupuestoRepository presupuestoRepo,
                                     PresupuestoItemRepository itemRepo,
                                     MailService mailService,
                                     PaymentApiService paymentApiService,
                                     PagoManualRepository pagoManualRepo,
                                     OrdenRepuestoService ordenRepuestoService) {
        this.solicitudRepo = solicitudRepo;
        this.tarifaRepo = tarifaRepo;
        this.presupuestoRepo = presupuestoRepo;
        this.itemRepo = itemRepo;
        this.mailService = mailService;
        this.paymentApiService = paymentApiService;
        this.pagoManualRepo = pagoManualRepo;
        this.ordenRepuestoService = ordenRepuestoService;
    }
    private static final Set<String> ETAPAS_PERMITEN_PAGO_FINAL = Set.of(
            "LISTO_RETIRAR",
            "ENTREGADO"
    );

    private boolean puedePagarFinalSegunEtapa(Presupuesto p) {
        if (p.getOtNroOrden() == null || p.getOtNroOrden().isBlank()) {
            // Si no hay OT asociada todavía, dejamos como estaba
            return true;
        }

        String etapa = ordenRepuestoService
                .getEtapaActualPorNroOrden(p.getOtNroOrden());

        String e = (etapa == null ? "" : etapa.toUpperCase());
        return ETAPAS_PERMITEN_PAGO_FINAL.contains(e);
    }

    // ─────────── Helpers ───────────
    private static BigDecimal calcularSena(BigDecimal total) {
        return total.multiply(BigDecimal.valueOf(0.30)).setScale(2, RoundingMode.HALF_UP);
    }
    private static BigDecimal porcentaje(BigDecimal total, double pct){
        return total.multiply(BigDecimal.valueOf(pct)).setScale(2, RoundingMode.HALF_UP);
    }
    // ─────────── Generación ───────────
    @Transactional
    public Presupuesto generarDesdeSolicitud(Long solicitudId,
                                             String vehiculoTipo,
                                             String piezaTipo,     // MOTOR | TAPA (se guarda para reportes)
                                             List<String> servicios,
                                             List<ExtraItemReq> extras) {
        if (solicitudId == null) {
            throw new IllegalArgumentException("solicitudId es obligatorio");
        }
        if (vehiculoTipo == null || vehiculoTipo.isBlank()) {
            throw new IllegalArgumentException("vehiculoTipo es obligatorio");
        }
        if (servicios == null || servicios.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un servicio");
        }

        // 1) Traer solicitud
        SolicitudPresupuesto s = solicitudRepo.findById(solicitudId)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada: " + solicitudId));

        // 2) No permitir más de un presupuesto por solicitud
        if (presupuestoRepo.existsBySolicitudId(solicitudId)) {
            throw new IllegalStateException("Ya existe un presupuesto para la solicitud " + solicitudId);
        }

        // 3) Validar que la solicitud tenga datos mínimos (marca/modelo)
        if (isBlank(s.getMarca()) || isBlank(s.getModelo())) {
            throw new IllegalStateException("No se puede generar presupuesto: la solicitud no tiene marca y modelo completos.");
        }

        // 4) Normalizar pieza según la solicitud (MOTOR / TAPA)
        String unidadSolicitud = s.getTipoUnidad() == null
                ? ""
                : s.getTipoUnidad().trim().toUpperCase();

        String piezaNormalizada;
        if (piezaTipo == null || piezaTipo.isBlank()) {
            // si no viene nada desde el front, usamos lo de la solicitud
            piezaNormalizada = unidadSolicitud;
        } else {
            piezaNormalizada = piezaTipo.trim().toUpperCase();
            // si en la solicitud hay tipo definido, deben coincidir
            if (!unidadSolicitud.isBlank() && !piezaNormalizada.equals(unidadSolicitud)) {
                throw new IllegalArgumentException(
                        "La pieza seleccionada (" + piezaNormalizada + ") no coincide con la unidad de la solicitud (" + unidadSolicitud + ")."
                );
            }
        }

        // 5) Normalizar vehículo
        String vt = vehiculoTipo.trim().toUpperCase();

        // normalizar nombres de servicios
        List<String> nombres = servicios.stream()
                .map(v -> v == null ? null : v.trim())
                .filter(v -> v != null && !v.isEmpty())
                .toList();

        // buscar tarifas para (vehiculoTipo + nombreServicio)
        List<ServicioTarifa> tarifas = tarifaRepo.findByVehiculoTipoAndNombreServicioIn(vt, nombres);

        // validar faltantes
        Set<String> hallados = tarifas.stream()
                .map(ServicioTarifa::getNombreServicio)
                .collect(Collectors.toSet());

        List<String> faltan = new ArrayList<>();
        for (String n : nombres) {
            if (!hallados.contains(n)) faltan.add(n);
        }
        if (!faltan.isEmpty()) {
            throw new IllegalArgumentException("Servicios sin tarifa para " + vt + ": " + String.join(", ", faltan));
        }

        // total base (tarifas existentes)
        BigDecimal total = tarifas.stream()
                .map(ServicioTarifa::getPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // sumar extras (si vienen)
        if (extras != null && !extras.isEmpty()) {
            for (ExtraItemReq ex : extras) {
                if (ex == null || ex.nombre() == null || ex.nombre().isBlank()) {
                    throw new IllegalArgumentException("Extra sin nombre");
                }
                if (ex.precio() == null || ex.precio().setScale(2, RoundingMode.HALF_UP).signum() <= 0) {
                    throw new IllegalArgumentException("El extra '" + ex.nombre() + "' debe tener precio > 0");
                }
                total = total.add(ex.precio().setScale(2, RoundingMode.HALF_UP));
            }
        }

        // crear presupuesto
        Presupuesto p = new Presupuesto();
        p.setSolicitudId(s.getId());
        p.setClienteNombre(s.getClienteNombre());
        p.setClienteEmail(s.getClienteEmail());
        p.setVehiculoTipo(vt);
        p.setPiezaTipo(piezaNormalizada); // <- ya viene validada contra la solicitud
        p.setEstado("PENDIENTE");
        p.setTotal(total);
        p = presupuestoRepo.save(p);

        // ítems por tarifa
        for (ServicioTarifa t : tarifas) {
            PresupuestoItem it = new PresupuestoItem();
            it.setPresupuesto(p);
            it.setServicioNombre(t.getNombreServicio());
            it.setPrecioUnitario(t.getPrecio());
            itemRepo.save(it);
        }

        // ítems extra
        if (extras != null && !extras.isEmpty()) {
            for (ExtraItemReq ex : extras) {
                PresupuestoItem it = new PresupuestoItem();
                it.setPresupuesto(p);
                it.setServicioNombre(ex.nombre().trim());
                it.setPrecioUnitario(ex.precio().setScale(2, RoundingMode.HALF_UP));
                itemRepo.save(it);
            }
        }

        return p;
    }

    /** helper chiquito al final de la clase (si no lo tenés ya) */
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }


    // ─────────── Consultas ───────────
    @Transactional(readOnly = true)
    public Presupuesto getById(Long id) {
        return presupuestoRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Presupuesto no encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public List<Presupuesto> listar(String estado, Long solicitudId) {
        boolean hasEstado = (estado != null && !estado.isBlank());
        boolean hasSolicitud = (solicitudId != null);

        if (hasEstado && hasSolicitud) {
            return presupuestoRepo.findAllByEstadoAndSolicitudIdOrderByCreadaEnDesc(estado, solicitudId);
        }
        if (hasEstado) {
            return presupuestoRepo.findAllByEstadoOrderByCreadaEnDesc(estado);
        }
        if (hasSolicitud) {
            return presupuestoRepo.findAllBySolicitudIdOrderByCreadaEnDesc(solicitudId);
        }
        return presupuestoRepo.findAllByOrderByCreadaEnDesc();
    }

    // ─────────── Decisiones ───────────
    @Transactional
    public Presupuesto aprobar(Long id, String usuario, String nota) {
        Presupuesto p = getById(id);
        if (!"PENDIENTE".equals(p.getEstado()))
            throw new IllegalStateException("Solo se puede aprobar si está PENDIENTE");

        p.setEstado("APROBADO");
        p.setDecisionUsuario(usuario);
        p.setDecisionFecha(LocalDateTime.now());
        p.setDecisionMotivo(nota);
        Presupuesto saved = presupuestoRepo.save(p);
        mailService.enviarDecisionPresupuesto(saved);
        return saved;
    }

    @Transactional
    public Presupuesto rechazar(Long id, String usuario, String nota) {
        Presupuesto p = getById(id);
        if (!"PENDIENTE".equals(p.getEstado()))
            throw new IllegalStateException("Solo se puede rechazar si está PENDIENTE");

        p.setEstado("RECHAZADO");
        p.setDecisionUsuario(usuario);
        p.setDecisionFecha(LocalDateTime.now());
        p.setDecisionMotivo(nota);
        Presupuesto saved = presupuestoRepo.save(p);
        mailService.enviarDecisionPresupuesto(saved);
        return saved;
    }

    // ─────────── Pagos (Checkout API) ───────────
    @Transactional
    public Presupuesto cobrarSenaApi(Long presupuestoId, PagoApiReq req) {
        Presupuesto p = presupuestoRepo.findById(presupuestoId)
                .orElseThrow(() -> new EntityNotFoundException("No existe presupuesto: " + presupuestoId));

        if (!"APROBADO".equalsIgnoreCase(p.getEstado())) {
            throw new IllegalStateException("Solo se puede cobrar seña si el presupuesto está APROBADO.");
        }

        BigDecimal monto = calcularSena(p.getTotal());

        Map<String, Object> resp = paymentApiService.crearPago(
                p.getId(),
                monto,
                req
        );

        String status = String.valueOf(resp.getOrDefault("status", ""));
        String payId = String.valueOf(resp.getOrDefault("id", ""));
        String dateApproved = String.valueOf(resp.get("date_approved"));

        if ("approved".equalsIgnoreCase(status)) {
            p.setSenaEstado("ACREDITADA");
        } else if (p.getSenaEstado() == null || p.getSenaEstado().isBlank()) {
            p.setSenaEstado("PENDIENTE");
        }

        p.setSenaPaymentId(payId);
        p.setSenaPaymentStatus(status);
        if (dateApproved != null && !"null".equals(dateApproved)) {
            try {
                p.setSenaPaidAt(OffsetDateTime.parse(dateApproved).toLocalDateTime());
            } catch (Exception ignored) {}
        }
        p.setSenaMonto(monto);

        return presupuestoRepo.save(p);
    }

    @Transactional(readOnly = true)
    public PagoInfoDTO getPagoInfoPublico(Long presupuestoId) {
        var p = presupuestoRepo.findById(presupuestoId)
                .orElseThrow(() -> new EntityNotFoundException("No existe presupuesto: " + presupuestoId));

        BigDecimal montoSena = calcularSena(p.getTotal());
        BigDecimal sena = p.getSenaMonto() != null ? p.getSenaMonto() : BigDecimal.ZERO;
        BigDecimal fin  = p.getFinalMonto() != null ? p.getFinalMonto() : BigDecimal.ZERO;
        BigDecimal saldo = p.getTotal().subtract(sena).subtract(fin).setScale(2, RoundingMode.HALF_UP);

        boolean puedePagar = "APROBADO".equalsIgnoreCase(p.getEstado()) && saldo.compareTo(BigDecimal.ZERO) > 0;

        return new PagoInfoDTO(
                p.getId(),
                montoSena,
                p.getClienteEmail(),
                p.getEstado(),
                p.getSenaEstado(),
                p.getSenaPaymentStatus(),
                p.getSenaPaymentId(),
                p.getSenaPaidAt(),

                // FINAL
                p.getFinalEstado(),
                p.getFinalPaymentStatus(),
                p.getFinalPaymentId(),
                p.getFinalPaidAt(),

                // totales
                p.getTotal(),
                saldo,

                puedePagar
        );
    }

    // ─────────── Pagos Manuales (HU9) ───────────
    @Transactional
    public PagoManual registrarPagoManual(PagoManualReq req, String usuario) {
        if (req == null) throw new IllegalArgumentException("Body requerido");
        if (req.presupuestoId() == null) throw new IllegalArgumentException("presupuestoId requerido");
        if (req.tipo() == null || req.tipo().isBlank()) throw new IllegalArgumentException("tipo requerido");
        if (req.monto() == null || req.monto().compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("monto inválido");
        if (req.medio() == null || req.medio().isBlank()) throw new IllegalArgumentException("medio requerido");

        // ✅ referencia obligatoria para manuales
        final String referencia = (req.referencia() == null ? "" : req.referencia().trim());
        if (referencia.isEmpty()) {
            throw new IllegalArgumentException("La referencia es obligatoria (nro de recibo/operación).");
        }
        // opcional: recortar a 100 por si acaso (coincide con la columna)
        final String ref100 = referencia.length() > 100 ? referencia.substring(0, 100) : referencia;

        var p = presupuestoRepo.findById(req.presupuestoId())
                .orElseThrow(() -> new EntityNotFoundException("No existe presupuesto: " + req.presupuestoId()));

        if (!"APROBADO".equalsIgnoreCase(p.getEstado()))
            throw new IllegalStateException("Solo se registran pagos si el presupuesto está APROBADO.");

        final String tipo = req.tipo().toUpperCase().trim();
        final LocalDateTime fechaPago = (req.fechaPago() != null) ? req.fechaPago().atStartOfDay() : LocalDateTime.now();

        // Total de servicios (lo que vale el presupuesto original)
        BigDecimal totalServicios = p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO;

        // Total de repuestos (si tiene OT asociada)
        BigDecimal totalRepuestos = BigDecimal.ZERO;
        if (p.getOtNroOrden() != null && !p.getOtNroOrden().isBlank()) {
            // 👇 usamos el mismo service que ya usaste para la factura
            totalRepuestos = ordenRepuestoService.calcularTotalRepuestosPorNroOrden(p.getOtNroOrden());
            if (totalRepuestos == null) {
                totalRepuestos = BigDecimal.ZERO;
            }
        }

        if ("SENA".equals(tipo)) {
            if ("ACREDITADA".equalsIgnoreCase(String.valueOf(p.getSenaEstado()))) {
                throw new IllegalStateException("La seña ya está acreditada.");
            }

            // Seña SIEMPRE sobre total de servicios (sin repuestos)
            BigDecimal esperado = totalServicios
                    .multiply(BigDecimal.valueOf(0.30))
                    .setScale(2, RoundingMode.HALF_UP);

            if (req.monto().setScale(2, RoundingMode.HALF_UP).compareTo(esperado) != 0) {
                throw new IllegalArgumentException("Monto de seña inválido. Esperado: " + esperado);
            }

            p.setSenaEstado("ACREDITADA");
            p.setSenaMonto(esperado);
            p.setSenaPaymentStatus("manual");
            p.setSenaPaymentId(ref100);
            p.setSenaPaidAt(fechaPago);
            presupuestoRepo.save(p);

        } else if ("FINAL".equals(tipo)) {
            if (!"ACREDITADA".equalsIgnoreCase(String.valueOf(p.getSenaEstado()))) {
                throw new IllegalStateException("Para registrar pago FINAL primero debe estar acreditada la seña.");
            }
            if ("ACREDITADA".equalsIgnoreCase(String.valueOf(p.getFinalEstado()))) {
                throw new IllegalStateException("El pago FINAL ya está acreditado.");
            }
            // ✅ NUEVO: validar etapa de la OT si corresponde
            if (!puedePagarFinalSegunEtapa(p)) {
                throw new IllegalStateException(
                        "No se puede registrar el pago FINAL mientras la OT no esté LISTO_RETIRAR/ENTREGADO."
                );
            }
            // Seña registrada (si por algún motivo faltara, calculamos 30% de servicios como fallback)
            BigDecimal senaRegistrada = p.getSenaMonto() != null
                    ? p.getSenaMonto()
                    : totalServicios.multiply(BigDecimal.valueOf(0.30)).setScale(2, RoundingMode.HALF_UP);

            // Total REAL de la reparación = servicios + repuestos
            BigDecimal totalReal = totalServicios.add(totalRepuestos);

            // Monto esperado para FINAL = totalReal - seña
            BigDecimal esperado = totalReal.subtract(senaRegistrada).setScale(2, RoundingMode.HALF_UP);
            if (esperado.compareTo(BigDecimal.ZERO) < 0) {
                esperado = BigDecimal.ZERO;
            }

            BigDecimal montoReq = req.monto().setScale(2, RoundingMode.HALF_UP);
            BigDecimal diff = montoReq.subtract(esperado).abs();

            // tolerancia de 1 peso por si hay redondeos menores
            if (diff.compareTo(new BigDecimal("1.00")) > 0) {
                throw new IllegalArgumentException("Monto FINAL inválido. Esperado (aprox.): " + esperado);
            }

            p.setFinalEstado("ACREDITADA");
            // guardamos el monto realmente pagado (ya validado)
            p.setFinalMonto(montoReq);
            p.setFinalPaymentStatus("manual");
            p.setFinalPaymentId(ref100);
            p.setFinalPaidAt(fechaPago);
            presupuestoRepo.save(p);

        } else {
            throw new IllegalArgumentException("tipo debe ser SENA o FINAL");
        }

        // Auditoría
        PagoManual pm = new PagoManual();
        pm.setPresupuesto(p);
        pm.setTipo(tipo);
        pm.setMedio(req.medio());
        pm.setReferencia(ref100);
        pm.setMonto(req.monto().setScale(2, RoundingMode.HALF_UP));
        pm.setFechaPago(fechaPago);
        pm.setUsuario(usuario != null ? usuario : "admin");
        pm.setNota(req.nota());

        return pagoManualRepo.save(pm);
    }
}