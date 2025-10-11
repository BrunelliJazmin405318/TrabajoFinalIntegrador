package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.domain.Presupuesto;
import ar.edu.utn.tfi.domain.PresupuestoItem;
import ar.edu.utn.tfi.domain.ServicioTarifa;
import ar.edu.utn.tfi.domain.SolicitudPresupuesto;
import ar.edu.utn.tfi.repository.PresupuestoItemRepository;
import ar.edu.utn.tfi.repository.PresupuestoRepository;
import ar.edu.utn.tfi.repository.ServicioTarifaRepository;
import ar.edu.utn.tfi.repository.SolicitudPresupuestoRepository;
import jakarta.persistence.EntityNotFoundException;
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

    public PresupuestoGestionService(SolicitudPresupuestoRepository solicitudRepo,
                                     ServicioTarifaRepository tarifaRepo,
                                     PresupuestoRepository presupuestoRepo,
                                     PresupuestoItemRepository itemRepo,
                                     MailService mailService) {
        this.solicitudRepo = solicitudRepo;
        this.tarifaRepo = tarifaRepo;
        this.presupuestoRepo = presupuestoRepo;
        this.itemRepo = itemRepo;
        this.mailService = mailService;
    }

    @Transactional
    public Presupuesto generarDesdeSolicitud(Long solicitudId, String vehiculoTipo, List<String> servicios) {
        if (solicitudId == null) throw new IllegalArgumentException("solicitudId es obligatorio");
        if (vehiculoTipo == null || vehiculoTipo.isBlank()) throw new IllegalArgumentException("vehiculoTipo es obligatorio");
        if (servicios == null || servicios.isEmpty()) throw new IllegalArgumentException("Debe seleccionar al menos un servicio");

        vehiculoTipo = vehiculoTipo.trim().toUpperCase();
        List<String> nombres = servicios.stream().map(s -> s.trim().toUpperCase()).toList();

        // Traer solicitud
        SolicitudPresupuesto s = solicitudRepo.findById(solicitudId)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada: " + solicitudId));

        // Resolver tarifas
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

        // Crear encabezado en estado PENDIENTE
        Presupuesto p = new Presupuesto();
        p.setSolicitudId(s.getId());
        p.setClienteNombre(s.getClienteNombre());
        p.setClienteEmail(s.getClienteEmail());
        p.setVehiculoTipo(vehiculoTipo);
        p.setEstado("PENDIENTE");       // <-- antes era EN_REVISION
        p.setTotal(total);              // seteo explícito del total

        p = presupuestoRepo.save(p);    // persistimos encabezado

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
        if (solicitudId != null) return presupuestoRepo.findBySolicitudIdOrderByCreadaEnDesc(solicitudId);
        if (estado == null || estado.isBlank()) return presupuestoRepo.findAllByOrderByCreadaEnDesc();
        return presupuestoRepo.findByEstadoOrderByCreadaEnDesc(estado.trim().toUpperCase());
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
}
