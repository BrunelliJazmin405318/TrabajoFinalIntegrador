package ar.edu.utn.tfi.repository;

import ar.edu.utn.tfi.domain.OrdenEtapaHistorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OrdenEtapaHistorialRepository extends JpaRepository<OrdenEtapaHistorial, Long> {

    // ✅ Nuevo método: ordena según el orden lógico del catálogo de etapas
    @Query("""
        SELECT h
        FROM OrdenEtapaHistorial h
        JOIN EtapaCatalogo e ON e.codigo = h.etapaCodigo
        WHERE h.ordenId = :ordenId
        ORDER BY e.orden ASC, h.fechaInicio ASC
    """)
    List<OrdenEtapaHistorial> findByOrdenIdOrdenadoPorCatalogo(Long ordenId);

    // 🟢 Podés dejar los otros métodos existentes también
    List<OrdenEtapaHistorial> findByOrdenIdOrderByFechaInicioAsc(Long ordenId);

    Optional<OrdenEtapaHistorial> findTopByOrdenIdAndFechaFinIsNullOrderByFechaInicioDesc(Long ordenId);
}