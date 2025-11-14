package ar.edu.utn.tfi.web.dto;

import ar.edu.utn.tfi.domain.OrdenRepuesto;

import java.math.BigDecimal;

public record RepuestoDTO(
        Long id,
        String descripcion,
        BigDecimal cantidad,
        BigDecimal precioUnit,
        BigDecimal subtotal
) {
    public static RepuestoDTO from(OrdenRepuesto r) {
        return new RepuestoDTO(
                r.getId(),
                r.getDescripcion(),
                r.getCantidad(),
                r.getPrecioUnit(),
                r.getSubtotal()
        );
    }
}