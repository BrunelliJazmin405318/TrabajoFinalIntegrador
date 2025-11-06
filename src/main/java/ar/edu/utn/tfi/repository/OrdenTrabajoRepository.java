package ar.edu.utn.tfi.repository;
import ar.edu.utn.tfi.domain.OrdenTrabajo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
public interface OrdenTrabajoRepository extends JpaRepository<OrdenTrabajo, Long> {
    Optional<OrdenTrabajo> findByNroOrden(String nroOrden);

    // 👉 Proyección para (tipo, cantidad)
    interface TipoCantidad {
        String getTipo();   // 'MOTOR' | 'TAPA'
        Long getCnt();      // cantidad
    }

    // 👉 Conteo por tipo entre fechas
    @Query(value = """
        SELECT ut.tipo   AS tipo,
               COUNT(*)  AS cnt
        FROM orden_trabajo ot
        JOIN unidad_trabajo ut ON ut.id = ot.unidad_id
        WHERE ot.creada_en >= :desde
          AND ot.creada_en <  :hasta
        GROUP BY ut.tipo
        """, nativeQuery = true)
    List<TipoCantidad> contarPorTipoEntre(@Param("desde") LocalDateTime desde,
                                          @Param("hasta") LocalDateTime hasta);
}
