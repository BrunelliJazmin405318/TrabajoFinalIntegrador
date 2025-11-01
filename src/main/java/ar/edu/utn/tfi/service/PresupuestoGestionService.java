package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.domain.Presupuesto;
import ar.edu.utn.tfi.domain.PresupuestoItem;
import ar.edu.utn.tfi.domain.ServicioTarifa;
import ar.edu.utn.tfi.domain.SolicitudPresupuesto;
import ar.edu.utn.tfi.repository.PresupuestoItemRepository;
import ar.edu.utn.tfi.repository.PresupuestoRepository;
import ar.edu.utn.tfi.repository.ServicioTarifaRepository;
import ar.edu.utn.tfi.repository.SolicitudPresupuestoRepository;
import ar.edu.utn.tfi.service.Pagos.PaymentApiService;
import ar.edu.utn.tfi.web.dto.PagoApiReq;
import jakarta.persistence.EntityNotFoundException;
import org.flywaydb.core.internal.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PresupuestoGestionService {

    private final SolicitudPresupuestoRepository solicitudRepo;
    private final ServicioTarifaRepository tarifaRepo;
    private final PresupuestoRepository presupuestoRepo;
    private final PresupuestoItemRepository itemRepo;
    private final MailService mailService;
    private final PaymentApiService paymentApiService;

    public PresupuestoGestionService(SolicitudPresupuestoRepository solicitudRepo,
                                     ServicioTarifaRepository tarifaRepo,
                                     PresupuestoRepository presupuestoRepo,
                                     PresupuestoItemRepository itemRepo,
                                     MailService mailService,
                                     PaymentApiService paymentApiService) {
        this.solicitudRepo = solicitudRepo;
        this.tarifaRepo = tarifaRepo;
        this.presupuestoRepo = presupuestoRepo;
        this.itemRepo = itemRepo;
        this.mailService = mailService;
        this.paymentApiService = paymentApiService;
    }

    @Transactional
    public Presupuesto generarDesdeSolicitud(Long solicitudId, String vehiculoTipo, List<String> servicios) {
        if (solicitudId == null) throw new IllegalArgumentException("solicitudId es obligatorio");
        if (vehiculoTipo == null || vehiculoTipo.isBlank())
            throw new IllegalArgumentException("vehiculoTipo es obligatorio");
        if (servicios == null || servicios.isEmpty())
            throw new IllegalArgumentException("Debe seleccionar al menos un servicio");

        // El tipo de vehículo sí en mayúsculas (congruente con la V10)
        vehiculoTipo = vehiculoTipo.trim().toUpperCase();

        // NO cambiar el casing de los nombres: deben coincidir 1:1 con la DB (tienen acentos y mayúsculas/minúsculas)
        List<String> nombres = servicios.stream()
                .map(s -> s == null ? null : s.trim())
                .filter(s -> s != null && !s.isEmpty())
                .toList();

        // Traer solicitud
        SolicitudPresupuesto s = solicitudRepo.findById(solicitudId)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada: " + solicitudId));

        // Resolver tarifas exactas
        List<ServicioTarifa> tarifas = tarifaRepo.findByVehiculoTipoAndNombreServicioIn(vehiculoTipo, nombres);
        if (tarifas.size() != nombres.size()) {
            Set<String> hallados = new HashSet<>();
            tarifas.forEach(t -> hallados.add(t.getNombreServicio()));
            List<String> faltan = new ArrayList<>();
            for (String n : nombres) if (!hallados.contains(n)) faltan.add(n);
            throw new IllegalArgumentException("Servicios inexistentes para " + vehiculoTipo + ": " + String.join(", ", faltan));
        }

        // Calcular total antes de guardar
        BigDecimal total = BigDecimal.ZERO;
        for (ServicioTarifa t : tarifas) {
            total = total.add(t.getPrecio());
        }

        // Encabezado en PENDIENTE
        Presupuesto p = new Presupuesto();
        p.setSolicitudId(s.getId());
        p.setClienteNombre(s.getClienteNombre());
        p.setClienteEmail(s.getClienteEmail());
        p.setVehiculoTipo(vehiculoTipo);
        p.setEstado("PENDIENTE");
        p.setTotal(total);                 // total explícito para cumplir NOT NULL

        p = presupuestoRepo.save(p);       // persistimos encabezado para tener ID

        // Ítems
        for (ServicioTarifa t : tarifas) {
            PresupuestoItem it = new PresupuestoItem();
            it.setPresupuesto(p);
            it.setServicioNombre(t.getNombreServicio());
            it.setPrecioUnitario(t.getPrecio());
            itemRepo.save(it);
        }

        return p;
    }

    @Transactional(readOnly = true)
    public Presupuesto getById(Long id) {
        return presupuestoRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Presupuesto no encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public List<Presupuesto> listar(String estado, Long solicitudId) {
        boolean hasEstado = StringUtils.hasText(estado);
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
    @Transactional
    public Presupuesto cobrarSenaApi(Long presupuestoId, PagoApiReq req) {
        Presupuesto p = presupuestoRepo.findById(presupuestoId)
                .orElseThrow(() -> new EntityNotFoundException("No existe presupuesto: " + presupuestoId));

        if (!"APROBADO".equalsIgnoreCase(p.getEstado())) {
            throw new IllegalStateException("Solo se puede cobrar seña si el presupuesto está APROBADO.");
        }

        BigDecimal monto = p.getTotal()
                .multiply(BigDecimal.valueOf(0.30))
                .setScale(2, java.math.RoundingMode.HALF_UP);

        Map<String, Object> resp = paymentApiService.crearPago(
                monto,
                "Seña presupuesto #" + p.getId(),
                req.token(),
                req.paymentMethodId(),
                req.installments(),
                req.issuerId(),
                req.payerEmail()
        );

        String status = String.valueOf(resp.getOrDefault("status", ""));
        String payId = String.valueOf(resp.getOrDefault("id", ""));
        String dateApproved = String.valueOf(resp.get("date_approved"));

        if ("approved".equalsIgnoreCase(status)) {
            p.setSenaEstado("ACREDITADA");
        } else {
            p.setSenaEstado("PENDIENTE");
        }

        p.setSenaPaymentId(payId);
        p.setSenaPaymentStatus(status);
        if (dateApproved != null && !"null".equals(dateApproved)) {
            try {
                p.setSenaPaidAt(java.time.OffsetDateTime.parse(dateApproved).toLocalDateTime());
            } catch (Exception ignored) {}
        }
        p.setSenaMonto(monto);

        return presupuestoRepo.save(p);
    }
}
