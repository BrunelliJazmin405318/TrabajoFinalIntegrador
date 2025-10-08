package ar.edu.utn.tfi.repository;

import ar.edu.utn.tfi.domain.DemoraMotivo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DemoraMotivoRepository extends JpaRepository<DemoraMotivo, Long> {
    Optional<DemoraMotivo> findByCodigo(String codigo);
}