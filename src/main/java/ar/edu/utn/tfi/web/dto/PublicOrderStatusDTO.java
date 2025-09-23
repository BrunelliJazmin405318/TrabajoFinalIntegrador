package ar.edu.utn.tfi.web.dto;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PublicOrderStatusDTO(
        String nroOrden,
        String estadoActual,
        LocalDate garantiaDesde,
        LocalDate garantiaHasta,
        LocalDateTime creadaEn
) {}
