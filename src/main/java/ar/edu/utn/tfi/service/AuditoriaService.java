package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.domain.AuditoriaCambio;
import ar.edu.utn.tfi.domain.OrdenTrabajo;
import ar.edu.utn.tfi.repository.AuditoriaCambioRepository;
import ar.edu.utn.tfi.repository.OrdenTrabajoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditoriaService {

    private final AuditoriaCambioRepository repo;
    private final OrdenTrabajoRepository ordenRepo;

    public AuditoriaService(AuditoriaCambioRepository repo, OrdenTrabajoRepository ordenRepo) {
        this.repo = repo;
        this.ordenRepo = ordenRepo;
    }

    // Transacción separada y forzada a disco
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarCambio(Long ordenId, String campo, String anterior, String nuevo, String usuario) {
        AuditoriaCambio a = new AuditoriaCambio();
        a.setOrdenId(ordenId);
        a.setCampo(campo);
        a.setValorAnterior(anterior);
        a.setValorNuevo(nuevo);
        a.setUsuario(usuario);
        repo.saveAndFlush(a); // <- flush para forzar escritura inmediata
        System.out.println("✅ Auditoría INSERTADA para ordenId=" + ordenId + " campo=" + campo);
    }

    @Transactional(readOnly = true)
    public List<AuditoriaCambio> listarPorNro(String nro) {
        OrdenTrabajo ot = ordenRepo.findByNroOrden(nro)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada: " + nro));
        return repo.findByOrdenIdOrderByFechaDesc(ot.getId());
    }
}