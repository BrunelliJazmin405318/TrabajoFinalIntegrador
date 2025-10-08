package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.service.AuditoriaService;
import ar.edu.utn.tfi.service.OrderQueryService;
import ar.edu.utn.tfi.web.dto.AuditoriaDTO;
import ar.edu.utn.tfi.web.dto.OrderStageDTO;
import ar.edu.utn.tfi.web.dto.PublicOrderDetailsDTO;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/public/ordenes")
public class OrdenPublicController {
    private final OrderQueryService service;
    private final AuditoriaService auditoriaService;

    public OrdenPublicController(OrderQueryService service, AuditoriaService auditoriaService) {
        this.service = service;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping("/{nroOrden}/estado")
    public PublicOrderDetailsDTO estado(@PathVariable String nroOrden) {
        return service.getPublicDetailsByNro(nroOrden);
    }

    @GetMapping("/{nroOrden}/historial")
    public List<OrderStageDTO> historial(@PathVariable String nroOrden) {
        return service.getHistorialByNro(nroOrden);
    }

    // NUEVO: auditoría
    @GetMapping("/{nroOrden}/auditoria")
    public List<AuditoriaDTO> auditoria(@PathVariable String nroOrden) {
        return auditoriaService.listarPorNro(nroOrden)
                .stream()
                .map(AuditoriaDTO::from)
                .collect(Collectors.toList()); // usa Collectors si te daba lío con .toList()
    }
}