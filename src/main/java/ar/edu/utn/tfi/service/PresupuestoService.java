package ar.edu.utn.tfi.service;
import ar.edu.utn.tfi.domain.Presupuesto;
import ar.edu.utn.tfi.domain.SolicitudPresupuesto;
import ar.edu.utn.tfi.repository.PresupuestoRepository;
import ar.edu.utn.tfi.repository.SolicitudPresupuestoRepository;
import ar.edu.utn.tfi.web.dto.SolicitudCreateDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ar.edu.utn.tfi.service.NotificationService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.security.SecureRandom;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PresupuestoService {
    private static final int MAX_PIN_INTENTOS = 5;
    private static final int MINUTOS_BLOQUEO_PIN = 15;
    private static final int LARGO_PIN = 6;

    private final SolicitudPresupuestoRepository repo;
    private final PresupuestoRepository repository;
    private final NotificationService notificationService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    public PresupuestoService(SolicitudPresupuestoRepository repo,  PresupuestoRepository repository, NotificationService notificationService) {
        this.repo = repo;
        this.repository = repository;
        this.notificationService = notificationService;
    }

    // Público
    @Transactional
    public SolicitudPresupuesto crearSolicitud(SolicitudCreateDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }

        if (dto.clienteNombre() == null || dto.clienteNombre().isBlank()) {
            throw new IllegalArgumentException("clienteNombre es obligatorio");
        }
        if (dto.tipoUnidad() == null || dto.tipoUnidad().isBlank()) {
            throw new IllegalArgumentException("tipoUnidad es obligatorio (MOTOR|TAPA)");
        }

        SolicitudPresupuesto s = new SolicitudPresupuesto();
        s.setClienteNombre(dto.clienteNombre().trim());
        s.setClienteTelefono(dto.clienteTelefono());
        s.setClienteEmail(dto.clienteEmail());
        s.setTipoUnidad(dto.tipoUnidad().trim().toUpperCase());
        s.setMarca(dto.marca());
        s.setModelo(dto.modelo());
        s.setNroMotor(dto.nroMotor());
        s.setDescripcion(dto.descripcion());

        // ✅ Nuevo: tipoConsulta con default COTIZACION
        String tipoConsulta = dto.tipoConsulta();
        if (tipoConsulta == null || tipoConsulta.isBlank()) {
            tipoConsulta = "COTIZACION";
        } else {
            tipoConsulta = tipoConsulta.trim().toUpperCase();
        }
        s.setTipoConsulta(tipoConsulta);

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

        // 🔄 antes: sout mock
        notificationService.notificarDecisionSolicitud(saved);

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

        // 🔄 antes: sout mock
        notificationService.notificarDecisionSolicitud(saved);

        return saved;
    }
    public void exigirPin(Long solicitudId, String pin) {
        if (pin == null || pin.isBlank()) throw new IllegalStateException("PIN_REQUERIDO");
        verificarPinSolicitud(solicitudId, pin.trim());
    }
    @Transactional(readOnly = true)
    public void exigirPinPorOrden(String nroOrden, String pin) {
        Presupuesto p = repository.findByOtNroOrden(nroOrden)
                .orElseThrow(() -> new EntityNotFoundException("No existe presupuesto para la orden: " + nroOrden));

        exigirPin(p.getSolicitudId(), pin); // reutiliza tu lógica central
    }
    // Público (nuevo) - crea solicitud + guarda pin hash + expira
    @Transactional
    public SolicitudPresupuesto crearSolicitud(SolicitudCreateDTO dto, String pinPlano) {

        SolicitudPresupuesto s = crearSolicitud(dto);

        s.setPinHash(passwordEncoder.encode(pinPlano));
        s.setPinExpiraEn(LocalDateTime.now().plusMinutes(20));
        s.setPinIntentos(0);
        s.setPinBloqueadoHasta(null);

        // ✅ Guardamos cambios primero (para tener todo persistido)
        SolicitudPresupuesto saved = repo.save(s);

        // ✅ Notificación (WhatsApp mock a consola por ahora)
        notificationService.notificarPinSolicitud(
                saved.getClienteTelefono(),
                saved.getId(),
                pinPlano,
                20 // minutos
        );

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
    @Transactional
    public void verificarPinSolicitud(Long solicitudId, String pinPlano) {
        SolicitudPresupuesto s = getById(solicitudId);
        LocalDateTime ahora = LocalDateTime.now();

        // 0) Validación básica del PIN
        if (pinPlano == null || pinPlano.isBlank()) {
            throw new IllegalStateException("PIN_REQUERIDO");
        }

        pinPlano = pinPlano.trim();

        if (!pinPlano.matches("\\d{" + LARGO_PIN + "}")) {
            throw new IllegalStateException("PIN_INVALIDO");
        }

        // 1) Si estaba bloqueado pero el tiempo ya venció, limpiamos el bloqueo
        if (s.getPinBloqueadoHasta() != null && !s.getPinBloqueadoHasta().isAfter(ahora)) {
            s.setPinBloqueadoHasta(null);
            s.setPinIntentos(0);
            repo.save(s);
        }

        // 2) ¿Sigue bloqueado?
        if (s.getPinBloqueadoHasta() != null && s.getPinBloqueadoHasta().isAfter(ahora)) {
            throw new IllegalStateException("PIN_BLOQUEADO");
        }

        // 3) ¿Expiró?
        if (s.getPinExpiraEn() == null || s.getPinExpiraEn().isBefore(ahora)) {
            throw new IllegalStateException("PIN_EXPIRADO");
        }

        // 4) ¿Hash existe?
        if (s.getPinHash() == null || s.getPinHash().isBlank()) {
            throw new IllegalStateException("PIN_NO_CONFIGURADO");
        }

        // 5) Validar pin
        boolean ok = passwordEncoder.matches(pinPlano, s.getPinHash());

        if (ok) {
            s.setPinIntentos(0);
            s.setPinBloqueadoHasta(null);
            repo.save(s);
            return;
        }

        // 6) Sumar intento y eventualmente bloquear
        int intentos = (s.getPinIntentos() == null) ? 0 : s.getPinIntentos();
        intentos++;
        s.setPinIntentos(intentos);

        if (intentos >= MAX_PIN_INTENTOS) {
            s.setPinBloqueadoHasta(ahora.plusMinutes(MINUTOS_BLOQUEO_PIN));
        }

        repo.save(s);
        throw new IllegalStateException("PIN_INVALIDO");
    }
    @Transactional(readOnly = true)
    public Long getSolicitudIdByNroOrden(String nroOrden) {
        return repository.findByOtNroOrden(nroOrden)
                .map(Presupuesto::getSolicitudId)
                .orElseThrow(() -> new EntityNotFoundException("No existe presupuesto/OT para nroOrden: " + nroOrden));
    }
    @Transactional
    public String regenerarPinSolicitud(Long solicitudId, int minutos) {
        SolicitudPresupuesto s = getById(solicitudId);

        String pinPlano = String.format("%06d", random.nextInt(1_000_000));

        s.setPinHash(passwordEncoder.encode(pinPlano));
        s.setPinExpiraEn(LocalDateTime.now().plusMinutes(minutos));
        s.setPinIntentos(0);
        s.setPinBloqueadoHasta(null);

        repo.save(s);

        return pinPlano;
    }
    @Transactional
    public void reenviarPinSolicitud(Long solicitudId, int minutos) {

        String pinPlano = regenerarPinSolicitud(solicitudId, minutos);

        SolicitudPresupuesto solicitud = getById(solicitudId);

        notificationService.notificarReenvioPinSolicitud(
                solicitud.getClienteTelefono(),
                solicitud.getId(),
                pinPlano,
                minutos
        );
    }
    @Transactional
    public void reenviarPinOrden(String nroOrden, int minutos) {
        Presupuesto p = repository.findByOtNroOrden(nroOrden)
                .orElseThrow(() -> new EntityNotFoundException("No existe presupuesto para la orden: " + nroOrden));

        SolicitudPresupuesto solicitud = getById(p.getSolicitudId());

        String pinPlano = regenerarPinSolicitud(solicitud.getId(), minutos);

        notificationService.notificarReenvioPinOrden(
                solicitud.getClienteTelefono(),
                nroOrden,
                pinPlano,
                minutos
        );
    }
}
