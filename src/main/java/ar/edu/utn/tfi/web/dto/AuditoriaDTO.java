package ar.edu.utn.tfi.web.dto;

import ar.edu.utn.tfi.domain.AuditoriaCambio;

import java.time.LocalDateTime;

public record AuditoriaDTO(
        LocalDateTime fecha,
        String campo,
        String valorAnterior,
        String valorNuevo,
        String usuario
) {
    public static AuditoriaDTO from(AuditoriaCambio a) {
        return new AuditoriaDTO(
                a.getFecha(),
                a.getCampo(),
                a.getValorAnterior(),
                a.getValorNuevo(),
                a.getUsuario()
        );
    }
}