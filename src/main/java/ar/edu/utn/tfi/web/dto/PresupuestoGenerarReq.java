package ar.edu.utn.tfi.web.dto;

import java.util.List;

public record PresupuestoGenerarReq(
        Long solicitudId,          // obligatorio en esta versión
        String vehiculoTipo,       // CONVENCIONAL | IMPORTADO
        List<String> servicios,     // ej: ["RECTIFICADO","ENSAYO"]
        String piezaTipo, // "MOTOR" o "TAPA" (opcional)
        List<ExtraItemReq> extras // servicios custom con precio obligatorio
) {}
