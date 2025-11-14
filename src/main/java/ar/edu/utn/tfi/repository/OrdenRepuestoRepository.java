package ar.edu.utn.tfi.repository;

import ar.edu.utn.tfi.domain.OrdenRepuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface OrdenRepuestoRepository extends JpaRepository<OrdenRepuesto, Long> {

    List<OrdenRepuesto> findByOrdenIdOrderByIdAsc(Long ordenId);

    @Query("""
           select coalesce(sum(r.precioUnit * r.cantidad), 0)
           from OrdenRepuesto r
           where r.ordenId = :ordenId
           """)
    BigDecimal totalByOrdenId(@Param("ordenId") Long ordenId);
}