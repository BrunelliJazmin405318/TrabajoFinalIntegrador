package ar.edu.utn.tfi.web.dto;

import ar.edu.utn.tfi.domain.Presupuesto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PresupuestoDTO(
        Long id,
        Long solicitudId,
        String clienteNombre,
        String clienteEmail,
        String vehiculoTipo,
        List<PresupuestoItemDTO> items,
        BigDecimal total,
        String estado,
        LocalDateTime creadaEn,
        String decisionUsuario,
        LocalDateTime decisionFecha,
        String decisionMotivo
) {
    public static PresupuestoDTO from(Presupuesto p, List<PresupuestoItemDTO> items) {
        return new PresupuestoDTO(
                p.getId(),
                p.getSolicitudId(),
                p.getClienteNombre(),
                p.getClienteEmail(),
                p.getVehiculoTipo(),
                items,
                p.getTotal(),
                p.getEstado(),
                p.getCreadaEn(),
                p.getDecisionUsuario(),
                p.getDecisionFecha(),
                p.getDecisionMotivo()
        );
    }
}

