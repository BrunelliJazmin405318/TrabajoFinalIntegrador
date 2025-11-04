package ar.edu.utn.tfi.repository;

import ar.edu.utn.tfi.domain.FacturaMock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FacturaMockRepository extends JpaRepository<FacturaMock, Long> {
    Optional<FacturaMock> findByPresupuestoId(Long presupuestoId);
    List<FacturaMock> findByPresupuestoIdIn(Collection<Long> ids);
}