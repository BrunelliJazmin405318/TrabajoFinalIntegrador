// src/main/java/ar/edu/utn/tfi/web/dto/PagoManualReq.java
package ar.edu.utn.tfi.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


public record PagoManualReq(
        Long presupuestoId,
        String tipo,          // "SENA" | "FINAL"
        BigDecimal monto,
        String medio,         // "EFECTIVO" | "TRANSFERENCIA" | "TARJETA" | "OTRO"
        String referencia,    // nro recibo / operación
        LocalDate fechaPago,
        String nota
) {
}