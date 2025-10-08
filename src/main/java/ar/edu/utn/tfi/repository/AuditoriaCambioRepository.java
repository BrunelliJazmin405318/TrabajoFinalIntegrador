package ar.edu.utn.tfi.repository;

import ar.edu.utn.tfi.domain.AuditoriaCambio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaCambioRepository extends JpaRepository<AuditoriaCambio, Long> {
    List<AuditoriaCambio> findByOrdenIdOrderByFechaDesc(Long ordenId);
}
