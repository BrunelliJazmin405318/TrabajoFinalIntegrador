package ar.edu.utn.tfi.web.dto;

import java.math.BigDecimal;

public record PresupuestoItemDTO(
        String servicioNombre,
        BigDecimal precioUnitario
) {}

