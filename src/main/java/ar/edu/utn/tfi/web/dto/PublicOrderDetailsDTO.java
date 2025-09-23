package ar.edu.utn.tfi.web.dto;
import java.util.List;
public record PublicOrderDetailsDTO(
        PublicOrderStatusDTO estado,
        List<OrderStageDTO> historial) {
}
