// src/main/java/ar/edu/utn/tfi/repository/OrdenTrabajoReportRepository.java
package ar.edu.utn.tfi.repository;

import ar.edu.utn.tfi.domain.OrdenTrabajo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

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
    SELECT 
        COALESCE(UPPER(p.pieza_tipo), UPPER(ut.tipo)) AS tipo,
        COUNT(*) AS cnt
    FROM orden_trabajo ot
    JOIN unidad_trabajo ut ON ut.id = ot.unidad_id
    LEFT JOIN presupuesto p ON p.ot_nro_orden = ot.nro_orden
    WHERE ot.creada_en >= :desde
      AND ot.creada_en  < :hasta
      AND COALESCE(UPPER(p.pieza_tipo), UPPER(ut.tipo)) IN ('MOTOR', 'TAPA')
    GROUP BY COALESCE(UPPER(p.pieza_tipo), UPPER(ut.tipo))
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

    // --- Proyección para ranking de clientes frecuentes ---
    interface ClienteRanking {
        Long getClienteId();
        String getNombre();
        String getTelefono();
        Long getCantidad();
    }

    @Query(value = """
        SELECT c.id          AS cliente_id,
               c.nombre      AS nombre,
               c.telefono    AS telefono,
               COUNT(*)      AS cantidad
        FROM orden_trabajo ot
        JOIN unidad_trabajo ut ON ut.id = ot.unidad_id
        JOIN cliente c         ON c.id = ut.cliente_id
        WHERE ot.estado_actual = 'ENTREGADO'
          AND ot.creada_en >= :desde
          AND ot.creada_en  < :hasta
        GROUP BY c.id, c.nombre, c.telefono
        ORDER BY cantidad DESC
        """, nativeQuery = true)
    List<ClienteRanking> rankingClientesEntre(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable
    );
}
