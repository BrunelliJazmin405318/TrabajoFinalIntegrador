package ar.edu.utn.tfi.web;
import ar.edu.utn.tfi.service.OrderQueryService;
import ar.edu.utn.tfi.web.dto.OrderStageDTO;
import ar.edu.utn.tfi.web.dto.PublicOrderDetailsDTO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/public/ordenes")
public class OrdenPublicController {
    private final OrderQueryService service;

    public OrdenPublicController(OrderQueryService service) {
        this.service = service;
    }

    // HU1: estado + historial juntos
    @GetMapping("/{nroOrden}/estado")
    public PublicOrderDetailsDTO estado(@PathVariable String nroOrden) {
        return service.getPublicDetailsByNro(nroOrden);
    }

    // HU2: solo historial (útil si el frontend lo pide aparte)
    @GetMapping("/{nroOrden}/historial")
    public List<OrderStageDTO> historial(@PathVariable String nroOrden) {
        return service.getHistorialByNro(nroOrden);
    }
}
