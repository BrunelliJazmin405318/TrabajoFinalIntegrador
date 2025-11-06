// src/main/java/ar/edu/utn/tfi/repository/IngresosReportRepository.java
package ar.edu.utn.tfi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ar.edu.utn.tfi.domain.Presupuesto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface IngresosReportRepository extends JpaRepository<Presupuesto, Long> {

    interface IngresoMesTipo {
        String getMes();     // YYYY-MM
        String getTipo();    // SENA | FINAL
        BigDecimal getTotal();
    }

    @Query(value = """
        SELECT
          to_char(date_trunc('month', x.fecha), 'YYYY-MM') AS mes,
          x.tipo  AS tipo,      -- 'SENA' | 'FINAL'
          SUM(x.monto) AS total
        FROM (
          -- API / Tarjeta desde presupuesto (excluye manual)
          SELECT p.sena_paid_at  AS fecha, 'SENA'  AS tipo, p.sena_monto  AS monto
          FROM   presupuesto p
          WHERE  p.sena_estado = 'ACREDITADA'
            AND  COALESCE(p.sena_payment_status,'') <> 'manual'
            AND  p.sena_paid_at >= :desde AND p.sena_paid_at < :hasta

          UNION ALL
          SELECT p.final_paid_at AS fecha, 'FINAL' AS tipo, p.final_monto AS monto
          FROM   presupuesto p
          WHERE  p.final_estado = 'ACREDITADA'
            AND  COALESCE(p.final_payment_status,'') <> 'manual'
            AND  p.final_paid_at >= :desde AND p.final_paid_at < :hasta

          -- Pagos manuales
          UNION ALL
          SELECT pm.fecha_pago   AS fecha, pm.tipo AS tipo, pm.monto AS monto
          FROM   pago_manual pm
          WHERE  pm.fecha_pago >= :desde AND pm.fecha_pago < :hasta
        ) x
        GROUP BY 1,2
        ORDER BY 1,2
        """, nativeQuery = true)
    List<IngresoMesTipo> listarIngresos(LocalDateTime desde, LocalDateTime hasta);
}