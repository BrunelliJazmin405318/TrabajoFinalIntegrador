package ar.edu.utn.tfi.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoInfoDTO(
        Long presupuestoId,
        BigDecimal montoSena,
        String clienteEmail,
        String presupuestoEstado,   // PENDIENTE | APROBADO | RECHAZADO
        String senaEstado,          // PENDIENTE | ACREDITADA | (null)
        String senaPaymentStatus,   // approved | rejected | in_process | (null)
        String senaPaymentId,
        LocalDateTime senaPaidAt,
        boolean puedePagar          // true si presupuesto APROBADO y seña no acreditada
) {}