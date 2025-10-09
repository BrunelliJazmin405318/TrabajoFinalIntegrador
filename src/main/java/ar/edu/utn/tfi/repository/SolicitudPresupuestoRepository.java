package ar.edu.utn.tfi.repository;

import ar.edu.utn.tfi.domain.SolicitudPresupuesto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitudPresupuestoRepository extends JpaRepository<SolicitudPresupuesto, Long> {
    List<SolicitudPresupuesto> findByEstadoOrderByCreadaEnDesc(String estado);
    List<SolicitudPresupuesto> findAllByOrderByCreadaEnDesc();
}