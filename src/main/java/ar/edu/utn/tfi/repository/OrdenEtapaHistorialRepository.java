package ar.edu.utn.tfi.repository;
import ar.edu.utn.tfi.domain.OrdenEtapaHistorial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrdenEtapaHistorialRepository extends JpaRepository<OrdenEtapaHistorial, Long> {
    List<OrdenEtapaHistorial> findByOrdenIdOrderByFechaInicioAsc(Long ordenId);

    Optional<OrdenEtapaHistorial> findTopByOrdenIdAndFechaFinIsNullOrderByFechaInicioDesc(Long ordenId);
}
