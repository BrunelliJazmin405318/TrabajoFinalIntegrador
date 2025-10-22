package ar.edu.utn.tfi.repository;

import ar.edu.utn.tfi.domain.PresupuestoItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PresupuestoItemRepository extends JpaRepository<PresupuestoItem, Long> {
    List<PresupuestoItem> findByPresupuestoId(Long presupuestoId);
}

