package ar.edu.utn.tfi.web.dto;
import java.time.LocalDateTime;

public record OrderStageDTO(
        String etapaCodigo,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        String observacion,
        String usuario
) {}
