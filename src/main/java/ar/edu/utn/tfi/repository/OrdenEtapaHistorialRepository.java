package ar.edu.utn.tfi.repository;
import ar.edu.utn.tfi.domain.OrdenEtapaHistorial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
public interface OrdenEtapaHistorialRepository extends JpaRepository<OrdenEtapaHistorial, Long> {
    List<OrdenEtapaHistorial> findByOrdenIdOrderByFechaInicioAsc(Long ordenId);
}
