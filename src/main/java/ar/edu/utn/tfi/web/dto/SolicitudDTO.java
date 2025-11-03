package ar.edu.utn.tfi.web.dto;

import ar.edu.utn.tfi.domain.SolicitudPresupuesto;

import java.time.LocalDateTime;

public record SolicitudDTO(
        Long id,
        String clienteNombre,
        String clienteTelefono,
        String clienteEmail,
        String tipoUnidad,     // MOTOR | TAPA
        String marca,
        String modelo,
        String nroMotor,
        String descripcion,
        String estado,         // PENDIENTE | APROBADO | RECHAZADO
        LocalDateTime creadaEn,
        String decisionUsuario,
        LocalDateTime decisionFecha,
        String decisionMotivo
) {
    public static SolicitudDTO from(SolicitudPresupuesto s) {
        return new SolicitudDTO(
                s.getId(),
                s.getClienteNombre(),
                s.getClienteTelefono(),
                s.getClienteEmail(),
                s.getTipoUnidad(),
                s.getMarca(),
                s.getModelo(),
                s.getNroMotor(),
                s.getDescripcion(),
                s.getEstado(),
                s.getCreadaEn(),
                s.getDecisionUsuario(),
                s.getDecisionFecha(),
                s.getDecisionMotivo()
        );
    }
}