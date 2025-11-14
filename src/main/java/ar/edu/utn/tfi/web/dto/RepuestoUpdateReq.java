package ar.edu.utn.tfi.web.dto;

import java.math.BigDecimal;

public record RepuestoUpdateReq(
        BigDecimal cantidad,
        BigDecimal precioUnit
) {}
