// src/main/java/ar/edu/utn/tfi/service/CrearOrdenService.java
package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.domain.*;
import ar.edu.utn.tfi.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public record CreateOTReq(
            String clienteNombre,
            String clienteTelefono,
            String tipo,
            String marca,
            String modelo,
            String nroMotor
    ) {}

    public record CreateOTResp(Long ordenId, String nroOrden) {}

    @Transactional
    public CreateOTResp crearOT(CreateOTReq req, String usuario) {

        // ---- Validaciones básicas ----
        if (req.clienteNombre() == null || req.clienteNombre().isBlank()) {
            throw new IllegalArgumentException("Nombre de cliente obligatorio");
        }
        if (req.clienteTelefono() == null || req.clienteTelefono().isBlank()) {
            throw new IllegalArgumentException("Teléfono de cliente obligatorio");
        }
        if (req.tipo() == null ||
                !(req.tipo().equalsIgnoreCase("MOTOR") || req.tipo().equalsIgnoreCase("TAPA"))) {
            throw new IllegalArgumentException("Tipo de unidad inválido (MOTOR|TAPA)");
        }

        String nombre  = req.clienteNombre().trim();
        String telefono = req.clienteTelefono().trim();
        String tipoUnidad = req.tipo().trim().toUpperCase();

        // 1) Cliente: reusar por nombre+tel, o crear uno nuevo
        Cliente cliente = clienteRepo
                .findByNombreAndTelefono(nombre, telefono)
                .orElseGet(() -> {
                    Cliente c = new Cliente();
                    c.setNombre(nombre);
                    c.setTelefono(telefono);
                    return clienteRepo.save(c);
                });

        // 2) Unidad de trabajo: SIEMPRE creamos una nueva
        //    (así no se pisan entre clientes ni se mezclan OT)
        UnidadTrabajo unidad = new UnidadTrabajo();
        unidad.setCliente(cliente);
        unidad.setTipo(tipoUnidad);
        unidad.setMarca(req.marca());
        unidad.setModelo(req.modelo());
        unidad.setNroMotor(req.nroMotor());
        unidad = unidadRepo.save(unidad);

        // 3) Generar nro de orden (simple contador por cantidad + 1)
        long next = ordenRepo.count() + 1;
        String nro = "OT-" + String.format("%04d", next);

        // 4) Crear OrdenTrabajo en estado inicial
        OrdenTrabajo ot = new OrdenTrabajo();
        ot.setNroOrden(nro);
        ot.setUnidadId(unidad.getId());          // FK a unidad_trabajo
        ot.setEstadoActual("INGRESO");
        ot.setCreadaEn(LocalDateTime.now());
        ot = ordenRepo.save(ot);

        // 5) Abrir historial etapa INGRESO
        OrdenEtapaHistorial h = new OrdenEtapaHistorial();
        h.setOrdenId(ot.getId());
        h.setEtapaCodigo("INGRESO");
        h.setFechaInicio(LocalDateTime.now());
        h.setObservacion("Alta de orden");
        h.setUsuario(usuario);
        historialRepo.save(h);

        // 6) Notificación de ingreso
        notificationService.notificarIngresoOrden(ot, cliente);

        return new CreateOTResp(ot.getId(), ot.getNroOrden());
    }
}