package ar.edu.utn.tfi.repository;

import ar.edu.utn.tfi.domain.OrdenRepuesto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrdenRepuestoRepository extends JpaRepository<OrdenRepuesto, Long> {

    List<OrdenRepuesto> findByOrdenIdOrderByIdAsc(Long ordenId);
}