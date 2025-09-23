package ar.edu.utn.tfi.repository;
import ar.edu.utn.tfi.domain.OrdenTrabajo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
public interface OrdenTrabajoRepository extends JpaRepository<OrdenTrabajo, Long> {
    Optional<OrdenTrabajo> findByNroOrden(String nroOrden);
}
