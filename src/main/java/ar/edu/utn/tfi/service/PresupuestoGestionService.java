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

@Service
public class PresupuestoGestionService {

    private final SolicitudPresupuestoRepository solicitudRepo;
    private final ServicioTarifaRepository tarifaRepo;
    private final PresupuestoRepository presupuestoRepo;
    private final PresupuestoItemRepository itemRepo;
    private final MailService mailService;
    private final PaymentApiService paymentApiService;
    private final PagoManualRepository pagoManualRepo;

    public PresupuestoGestionService(SolicitudPresupuestoRepository solicitudRepo,
                                     ServicioTarifaRepository tarifaRepo,
                                     PresupuestoRepository presupuestoRepo,
                                     PresupuestoItemRepository itemRepo,
                                     MailService mailService,
                                     PaymentApiService paymentApiService,
                                     PagoManualRepository pagoManualRepo) {
        this.solicitudRepo = solicitudRepo;
        this.tarifaRepo = tarifaRepo;
        this.presupuestoRepo = presupuestoRepo;
        this.itemRepo = itemRepo;
        this.mailService = mailService;
        this.paymentApiService = paymentApiService;
        this.pagoManualRepo = pagoManualRepo;
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
    public Presupuesto generarDesdeSolicitud(Long solicitudId, String vehiculoTipo, List<String> servicios) {
        if (solicitudId == null) throw new IllegalArgumentException("solicitudId es obligatorio");
        if (vehiculoTipo == null || vehiculoTipo.isBlank())
            throw new IllegalArgumentException("vehiculoTipo es obligatorio");
        if (servicios == null || servicios.isEmpty())
            throw new IllegalArgumentException("Debe seleccionar al menos un servicio");

        vehiculoTipo = vehiculoTipo.trim().toUpperCase();

        List<String> nombres = servicios.stream()
                .map(s -> s == null ? null : s.trim())
                .filter(s -> s != null && !s.isEmpty())
                .toList();

        SolicitudPresupuesto s = solicitudRepo.findById(solicitudId)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada: " + solicitudId));

        var tarifas = tarifaRepo.findByVehiculoTipoAndNombreServicioIn(vehiculoTipo, nombres);
        if (tarifas.size() != nombres.size()) {
            Set<String> hallados = new HashSet<>();
            tarifas.forEach(t -> hallados.add(t.getNombreServicio()));
            List<String> faltan = new ArrayList<>();
            for (String n : nombres) if (!hallados.contains(n)) faltan.add(n);
            throw new IllegalArgumentException("Servicios inexistentes para " + vehiculoTipo + ": " + String.join(", ", faltan));
        }

        BigDecimal total = BigDecimal.ZERO;
        for (var t : tarifas) total = total.add(t.getPrecio());

        Presupuesto p = new Presupuesto();
        p.setSolicitudId(s.getId());
        p.setClienteNombre(s.getClienteNombre());
        p.setClienteEmail(s.getClienteEmail());
        p.setVehiculoTipo(vehiculoTipo);
        p.setEstado("PENDIENTE");
        p.setTotal(total);
        p = presupuestoRepo.save(p);

        for (var t : tarifas) {
            PresupuestoItem it = new PresupuestoItem();
            it.setPresupuesto(p);
            it.setServicioNombre(t.getNombreServicio());
            it.setPrecioUnitario(t.getPrecio());
            itemRepo.save(it);
        }
        return p;
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

        var p = presupuestoRepo.findById(req.presupuestoId())
                .orElseThrow(() -> new EntityNotFoundException("No existe presupuesto: " + req.presupuestoId()));

        if (!"APROBADO".equalsIgnoreCase(p.getEstado()))
            throw new IllegalStateException("Solo se registran pagos si el presupuesto está APROBADO.");

        final String tipo = req.tipo().toUpperCase().trim();

        // Convertimos LocalDate (opcional) a LocalDateTime
        final LocalDateTime fechaPago = (req.fechaPago() != null)
                ? req.fechaPago().atStartOfDay()
                : LocalDateTime.now();

        if ("SENA".equals(tipo)) {
            if ("ACREDITADA".equalsIgnoreCase(String.valueOf(p.getSenaEstado()))) {
                throw new IllegalStateException("La seña ya está acreditada.");
            }
            BigDecimal esperado = porcentaje(p.getTotal(), 0.30);
            if (req.monto().setScale(2, RoundingMode.HALF_UP).compareTo(esperado) != 0) {
                throw new IllegalArgumentException("Monto de seña inválido. Esperado: " + esperado);
            }
            p.setSenaEstado("ACREDITADA");
            p.setSenaMonto(esperado);
            p.setSenaPaymentStatus("manual");
            p.setSenaPaymentId(req.referencia());
            p.setSenaPaidAt(fechaPago);
            presupuestoRepo.save(p);

        } else if ("FINAL".equals(tipo)) {
            // ✅ Ya no exigimos seña previa: si no hay seña, el saldo es el total
            if ("ACREDITADA".equalsIgnoreCase(String.valueOf(p.getFinalEstado()))) {
                throw new IllegalStateException("El pago FINAL ya está acreditado.");
            }

            BigDecimal senaAcreditada = (p.getSenaMonto() != null) ? p.getSenaMonto() : BigDecimal.ZERO;
            BigDecimal esperado = p.getTotal()
                    .subtract(senaAcreditada)
                    .setScale(2, RoundingMode.HALF_UP);

            if (req.monto().setScale(2, RoundingMode.HALF_UP).compareTo(esperado) != 0) {
                throw new IllegalArgumentException("Monto FINAL inválido. Esperado: " + esperado);
            }

            p.setFinalEstado("ACREDITADA");
            p.setFinalMonto(esperado);
            p.setFinalPaymentStatus("manual");
            p.setFinalPaymentId(req.referencia());
            p.setFinalPaidAt(fechaPago);
            presupuestoRepo.save(p);

        } else {
            throw new IllegalArgumentException("tipo debe ser SENA o FINAL");
        }

        // Auditoría
        PagoManual pm = new PagoManual();
        pm.setPresupuesto(p);
        pm.setTipo(tipo);
        pm.setMedio(req.medio().trim());
        pm.setReferencia(req.referencia());
        pm.setMonto(req.monto().setScale(2, RoundingMode.HALF_UP));
        pm.setFechaPago(fechaPago);
        pm.setUsuario((usuario != null && !usuario.isBlank()) ? usuario : "admin");
        pm.setNota(req.nota());

        return pagoManualRepo.save(pm);
    }
}