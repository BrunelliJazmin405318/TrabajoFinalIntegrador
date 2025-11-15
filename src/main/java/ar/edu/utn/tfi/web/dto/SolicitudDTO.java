package ar.edu.utn.tfi.web.dto;

import ar.edu.utn.tfi.domain.SolicitudPresupuesto;

import java.time.LocalDateTime;

public record SolicitudDTO(
        Long id,
        String clienteNombre,
        String clienteTelefono,
        String clienteEmail,
        String tipoUnidad,
        String marca,
        String modelo,
        String nroMotor,
        String descripcion,
        String tipoConsulta,       // ✅ NUEVO
        String estado,
        LocalDateTime creadaEn,
        boolean presupuestoGenerado
) {
    public static SolicitudDTO from(SolicitudPresupuesto s, boolean presupuestoGenerado) {
        String tipo = s.getTipoConsulta();
        if (tipo == null || tipo.isBlank()) {
            tipo = "COTIZACION"; // default para registros viejos
        }

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
                tipo,
                s.getEstado(),
                s.getCreadaEn(),
                presupuestoGenerado
        );
    }
}