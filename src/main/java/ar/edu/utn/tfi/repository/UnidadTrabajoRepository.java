package ar.edu.utn.tfi.repository;

import ar.edu.utn.tfi.domain.UnidadTrabajo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UnidadTrabajoRepository extends JpaRepository<UnidadTrabajo, Long> {
    Optional<UnidadTrabajo> findByNroMotor(String nroMotor);
}