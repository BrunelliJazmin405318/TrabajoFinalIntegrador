package ar.edu.utn.tfi.repository;

import ar.edu.utn.tfi.domain.OrdenTrabajo;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface OrdenTrabajoReportRepository extends JpaRepository<OrdenTrabajo, Long> {

    // ---- Proyección para /motores-vs-tapas
    interface TipoCantidad {
        String getTipo();   // 'MOTOR' | 'TAPA'
        Long getCnt();
    }

    @Query(value = """
        SELECT ut.tipo AS tipo, COUNT(*) AS cnt
        FROM orden_trabajo ot
        JOIN unidad_trabajo ut ON ut.id = ot.unidad_id
        WHERE ot.creada_en >= :desde
          AND ot.creada_en  < :hasta
        GROUP BY ut.tipo
        """, nativeQuery = true)
    List<TipoCantidad> contarPorTipoEntre(@Param("desde") LocalDateTime desde,
                                          @Param("hasta") LocalDateTime hasta);


    @Query(value = """
        SELECT ot.estado_actual AS etapa, COUNT(*) AS cnt
        FROM orden_trabajo ot
        JOIN unidad_trabajo ut   ON ut.id = ot.unidad_id
        LEFT JOIN etapa_catalogo ec ON ec.codigo = ot.estado_actual
        WHERE ut.tipo = 'MOTOR'
          AND ot.creada_en >= :desde
          AND ot.creada_en <  :hasta
        GROUP BY ot.estado_actual, ec.orden
        ORDER BY COALESCE(ec.orden, 999)
        """, nativeQuery = true)
    List<Map<String,Object>> contarMotoresPorEtapaRango(@Param("desde") LocalDateTime desde,
                                                        @Param("hasta") LocalDateTime hasta);

}