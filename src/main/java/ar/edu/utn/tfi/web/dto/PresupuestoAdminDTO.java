// ar/edu/utn/tfi/web/dto/PresupuestoAdminDTO.java
package ar.edu.utn.tfi.web.dto;

import ar.edu.utn.tfi.domain.Presupuesto;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PresupuestoAdminDTO(
        Long id,
        Long solicitudId,
        String clienteNombre,
        String clienteEmail,
        String vehiculoTipo,
        BigDecimal total,
        String estado,
        LocalDateTime creadaEn,

        // Seña
        String senaEstado,
        BigDecimal senaMonto,
        LocalDateTime senaPaidAt,
        String senaPaymentId,
        String senaPaymentStatus,

        // Final
        String finalEstado,
        BigDecimal finalMonto,
        LocalDateTime finalPaidAt,
        String finalPaymentId,
        String finalPaymentStatus,

        // Factura
        String facturaNumero,
        String facturaTipo,

        // ✅ NUEVO: número de OT persistido en la tabla presupuesto
        String otNroOrden
) {
    public static PresupuestoAdminDTO from(Presupuesto p) {
        return from(p, null, null);
    }

    public static PresupuestoAdminDTO from(Presupuesto p, String facturaNumero, String facturaTipo) {
        return new PresupuestoAdminDTO(
                p.getId(),
                p.getSolicitudId(),
                p.getClienteNombre(),
                p.getClienteEmail(),
                p.getVehiculoTipo(),
                p.getTotal(),
                p.getEstado(),
                p.getCreadaEn(),

                // Seña
                p.getSenaEstado(),
                p.getSenaMonto(),
                p.getSenaPaidAt(),
                p.getSenaPaymentId(),
                p.getSenaPaymentStatus(),

                // Final
                p.getFinalEstado(),
                p.getFinalMonto(),
                p.getFinalPaidAt(),
                p.getFinalPaymentId(),
                p.getFinalPaymentStatus(),

                // Factura
                facturaNumero,
                facturaTipo,

                // ✅ NUEVO
                p.getOtNroOrden()
        );
    }
}