package ar.edu.utn.tfi.service;
import ar.edu.utn.tfi.domain.Presupuesto;
import ar.edu.utn.tfi.domain.SolicitudPresupuesto;
import ar.edu.utn.tfi.repository.PresupuestoRepository;
import ar.edu.utn.tfi.repository.SolicitudPresupuestoRepository;
import ar.edu.utn.tfi.web.dto.SolicitudCreateDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PresupuestoService {
    private final SolicitudPresupuestoRepository repo;
    private final PresupuestoRepository repository;

    public PresupuestoService(SolicitudPresupuestoRepository repo,  PresupuestoRepository repository) {
        this.repo = repo;
        this.repository = repository;
    }

    // Público
    @Transactional
    public SolicitudPresupuesto crearSolicitud(SolicitudCreateDTO dto) {
        if (dto.clienteNombre() == null || dto.clienteNombre().isBlank())
            throw new IllegalArgumentException("clienteNombre es obligatorio");
        if (dto.tipoUnidad() == null || dto.tipoUnidad().isBlank())
            throw new IllegalArgumentException("tipoUnidad es obligatorio (MOTOR|TAPA)");

        SolicitudPresupuesto s = new SolicitudPresupuesto();
        s.setClienteNombre(dto.clienteNombre().trim());
        s.setClienteTelefono(dto.clienteTelefono());
        s.setClienteEmail(dto.clienteEmail());
        s.setTipoUnidad(dto.tipoUnidad().trim().toUpperCase());
        s.setMarca(dto.marca());
        s.setModelo(dto.modelo());
        s.setNroMotor(dto.nroMotor());
        s.setDescripcion(dto.descripcion());
        s.setEstado("PENDIENTE");
        return repo.save(s);
    }

    // Público
    @Transactional(readOnly = true)
    public SolicitudPresupuesto getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada: " + id));
    }

    // Admin: listar
    @Transactional(readOnly = true)
    public List<SolicitudPresupuesto> listar(String estado) {
        if (estado == null || estado.isBlank()) return repo.findAllByOrderByCreadaEnDesc();
        return repo.findByEstadoOrderByCreadaEnDesc(estado.trim().toUpperCase());
    }

    // Admin: aprobar
    @Transactional
    public SolicitudPresupuesto aprobar(Long id, String usuario, String nota) {
        SolicitudPresupuesto s = getById(id);
        if (!"PENDIENTE".equals(s.getEstado())) {
            throw new IllegalStateException("Solo se puede aprobar si está PENDIENTE");
        }
        s.setEstado("APROBADO");
        s.setDecisionUsuario(usuario);
        s.setDecisionFecha(LocalDateTime.now());
        s.setDecisionMotivo(nota);
        SolicitudPresupuesto saved = repo.save(s);

        // Mock WhatsApp
        System.out.println("📲 [WA MOCK] Aprobada solicitud #" + saved.getId() +
                " - Cliente: " + saved.getClienteNombre() +
                " - Tel: " + saved.getClienteTelefono());

        return saved;
    }

    // Admin: rechazar
    @Transactional
    public SolicitudPresupuesto rechazar(Long id, String usuario, String nota) {
        SolicitudPresupuesto s = getById(id);
        if (!"PENDIENTE".equals(s.getEstado())) {
            throw new IllegalStateException("Solo se puede rechazar si está PENDIENTE");
        }
        s.setEstado("RECHAZADO");
        s.setDecisionUsuario(usuario);
        s.setDecisionFecha(LocalDateTime.now());
        s.setDecisionMotivo(nota);
        SolicitudPresupuesto saved = repo.save(s);

        // Mock WhatsApp
        System.out.println("📲 [WA MOCK] Rechazada solicitud #" + saved.getId() +
                " - Cliente: " + saved.getClienteNombre() +
                " - Tel: " + saved.getClienteTelefono());

        return saved;
    }

    public List<Presupuesto> listar(String estado, Long solicitudId) {
        boolean tieneEstado = estado != null && !estado.isBlank();
        boolean tieneSid = solicitudId != null;

        if (tieneEstado && tieneSid) {
            return repository.findAllByEstadoAndSolicitudIdOrderByCreadaEnDesc(estado, solicitudId);
        } else if (tieneEstado) {
            return repository.findAllByEstadoOrderByCreadaEnDesc(estado);
        } else if (tieneSid) {
            return repository.findAllBySolicitudIdOrderByCreadaEnDesc(solicitudId);
        } else {
            return repository.findAllByOrderByCreadaEnDesc();
        }
    }
}
