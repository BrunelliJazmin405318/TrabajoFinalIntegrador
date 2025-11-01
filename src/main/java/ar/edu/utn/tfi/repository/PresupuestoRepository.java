package ar.edu.utn.tfi.repository;

import ar.edu.utn.tfi.domain.Presupuesto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PresupuestoRepository extends JpaRepository<Presupuesto, Long> {

    List<Presupuesto> findAllByOrderByCreadaEnDesc();
    List<Presupuesto> findAllByEstadoOrderByCreadaEnDesc(String estado);
    List<Presupuesto> findAllBySolicitudIdOrderByCreadaEnDesc(Long solicitudId);
    List<Presupuesto> findAllByEstadoAndSolicitudIdOrderByCreadaEnDesc(String estado, Long solicitudId);

    Optional<Presupuesto> findBySenaPreferenceId(String senaPreferenceId);
}