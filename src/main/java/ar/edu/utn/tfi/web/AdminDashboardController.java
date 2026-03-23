package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.repository.OrdenTrabajoRepository;
import ar.edu.utn.tfi.repository.PresupuestoRepository;
import ar.edu.utn.tfi.repository.SolicitudPresupuestoRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AdminDashboardController {

    private final OrdenTrabajoRepository ordenRepo;
    private final SolicitudPresupuestoRepository solicitudRepo;
    private final PresupuestoRepository presupuestoRepo;

    public AdminDashboardController(
            OrdenTrabajoRepository ordenRepo,
            SolicitudPresupuestoRepository solicitudRepo,
            PresupuestoRepository presupuestoRepo
    ) {
        this.ordenRepo = ordenRepo;
        this.solicitudRepo = solicitudRepo;
        this.presupuestoRepo = presupuestoRepo;
    }

    @GetMapping("/admin/dashboard")
    public Map<String, Object> dashboard() {

        long ordenesActivas = ordenRepo.count();
        long listasRetirar = ordenRepo.countByEstadoActual("LISTO_RETIRAR");
        long solicitudesPendientes = solicitudRepo.count();
        long presupuestosPendientes = presupuestoRepo.count();

        return Map.of(
                "ordenesActivas", ordenesActivas,
                "listasRetirar", listasRetirar,
                "solicitudesPendientes", solicitudesPendientes,
                "presupuestosPendientes", presupuestosPendientes
        );
    }
}