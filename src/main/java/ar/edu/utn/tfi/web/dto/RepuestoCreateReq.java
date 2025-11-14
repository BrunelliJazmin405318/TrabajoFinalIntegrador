package ar.edu.utn.tfi.web.dto;

import java.math.BigDecimal;

public record RepuestoCreateReq(
        String descripcion,
        BigDecimal cantidad,
        BigDecimal precioUnit
) {}