// src/main/java/ar/edu/utn/tfi/service/OrderCreationService.java
package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.domain.*;
import ar.edu.utn.tfi.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ar.edu.utn.tfi.service.NotificationService;
import ar.edu.utn.tfi.domain.Cliente;

import java.time.LocalDateTime;

@Service
public class CrearOrdenService {
    private final ClienteRepository clienteRepo;
    private final UnidadTrabajoRepository unidadRepo;
    private final OrdenTrabajoRepository ordenRepo;
    private final OrdenEtapaHistorialRepository historialRepo;
    private final NotificationService notificationService;

    public CrearOrdenService(ClienteRepository clienteRepo,
                                UnidadTrabajoRepository unidadRepo,
                                OrdenTrabajoRepository ordenRepo,
                                OrdenEtapaHistorialRepository historialRepo,
                             NotificationService notificationService) {
        this.clienteRepo = clienteRepo;
        this.unidadRepo = unidadRepo;
        this.ordenRepo = ordenRepo;
        this.historialRepo = historialRepo;
        this.notificationService = notificationService;
    }

    // DTO simple para el request
    public record CreateOTReq(String clienteNombre, String clienteTelefono,
                              String tipo, String marca, String modelo, String nroMotor) {}

    public record CreateOTResp(Long ordenId, String nroOrden) {}

    @Transactional
    public CreateOTResp crearOT(CreateOTReq req, String usuario) {
        if (req.clienteNombre() == null || req.clienteNombre().isBlank())
            throw new IllegalArgumentException("Nombre de cliente obligatorio");
        if (req.tipo() == null || !(req.tipo().equalsIgnoreCase("MOTOR") || req.tipo().equalsIgnoreCase("TAPA")))
            throw new IllegalArgumentException("Tipo de unidad inválido (MOTOR|TAPA)");
        // 1) Cliente (upsert por nombre+tel)
        var cliente = clienteRepo.findByNombreAndTelefono(req.clienteNombre(), req.clienteTelefono())
                .orElseGet(() -> {
                    var c = new Cliente();
                    c.setNombre(req.clienteNombre());
                    c.setTelefono(req.clienteTelefono());
                    return clienteRepo.save(c);
                });

        // 2) Unidad (upsert por nro motor)
        var unidad = (req.nroMotor() != null && !req.nroMotor().isBlank())
                ? unidadRepo.findByNroMotor(req.nroMotor()).orElse(null)
                : null;

        if (unidad == null) {
            unidad = new UnidadTrabajo();
            unidad.setCliente(cliente);
            unidad.setTipo(req.tipo().toUpperCase());
            unidad.setMarca(req.marca());
            unidad.setModelo(req.modelo());
            unidad.setNroMotor(req.nroMotor());
            unidad = unidadRepo.save(unidad);
        } else {
            // asegura asociación de cliente si faltaba
            if (unidad.getCliente() == null) {
                unidad.setCliente(cliente);
                unidad = unidadRepo.save(unidad);
            }
        }

        // 3) Generar nro de orden (simple contador por cantidad + 1)
        long next = ordenRepo.count() + 1;
        String nro = "OT-" + String.format("%04d", next);

        // 4) Crear OrdenTrabajo en estado inicial
        var ot = new OrdenTrabajo();
        ot.setNroOrden(nro);
        // si tu entidad usa unidadId (Long) en lugar de relación:
        ot.setUnidadId(unidad.getId());
        ot.setEstadoActual("INGRESO");
        ot.setCreadaEn(LocalDateTime.now());
        ot = ordenRepo.save(ot);

        // 5) Abrir historial etapa INGRESO
        var h = new OrdenEtapaHistorial();
        h.setOrdenId(ot.getId());
        h.setEtapaCodigo("INGRESO");
        h.setFechaInicio(LocalDateTime.now());
        h.setObservacion("Alta de orden");
        h.setUsuario(usuario);
        historialRepo.save(h);
        // 📲 notificación de ingreso al taller
        notificationService.notificarIngresoOrden(ot, cliente);

        return new CreateOTResp(ot.getId(), ot.getNroOrden());
    }
}