package ar.edu.utn.tfi.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PresupuestoListDTO(
        Long id,
        Long solicitudId,
        String clienteNombre,
        String clienteEmail,
        String vehiculoTipo,
        BigDecimal total,
        String estado,
        LocalDateTime creadaEn,

        // --- Campos para mostrar estado y link de seña ---
        String senaEstado,
        BigDecimal senaMonto,
        LocalDateTime senaPaidAt,
        String senaPaymentId,
        String senaPaymentStatus
) {}
