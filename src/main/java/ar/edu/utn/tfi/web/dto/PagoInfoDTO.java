// src/main/java/ar/edu/utn/tfi/web/dto/PagoInfoDTO.java
package ar.edu.utn.tfi.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoInfoDTO(
        Long presupuestoId,
        BigDecimal montoSena,
        String clienteEmail,
        String presupuestoEstado,   // PENDIENTE | APROBADO | RECHAZADO
        String senaEstado,          // PENDIENTE | ACREDITADA | (null)
        String senaPaymentStatus,   // approved | rejected | in_process | manual | (null)
        String senaPaymentId,
        LocalDateTime senaPaidAt,

        // 🆕 FINAL
        String finalEstado,         // ACREDITADA | (null)
        String finalPaymentStatus,  // manual | approved | ...
        String finalPaymentId,
        LocalDateTime finalPaidAt,

        // 🆕 TOTALES
        BigDecimal total,
        BigDecimal saldoRestante,

        boolean puedePagar          // true si queda saldo por pagar y corresponde mostrar botón
) {}