package ar.edu.utn.tfi.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController

public class AdminHealthController {
    @GetMapping("/admin/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
