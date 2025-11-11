// src/main/java/ar/edu/utn/tfi/web/dto/ClienteRankingDTO.java
package ar.edu.utn.tfi.web.dto;

public record ClienteRankingDTO(
        Long clienteId,
        String clienteNombre,
        String telefono,
        Long cantOrdenes
) {}
