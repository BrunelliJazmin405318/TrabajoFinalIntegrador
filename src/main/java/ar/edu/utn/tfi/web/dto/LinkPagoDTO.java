// src/main/java/ar/edu/utn/tfi/web/dto/LinkPagoDTO.java
package ar.edu.utn.tfi.web.dto;

import java.math.BigDecimal;

public record LinkPagoDTO(Long presupuestoId, BigDecimal monto, String url) {}
