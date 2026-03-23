package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.security.JwtService;
import ar.edu.utn.tfi.service.PresupuestoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/public/auth")
public class PublicAuthController {

    private final PresupuestoService presupuestoService;
    private final JwtService jwtService;
    private static final int PIN_TTL_MINUTES = 20;

    public PublicAuthController(PresupuestoService presupuestoService, JwtService jwtService) {
        this.presupuestoService = presupuestoService;
        this.jwtService = jwtService;
    }

    public record PinReq(String pin) {}

    @PostMapping("/solicitud/{solicitudId}")
    public ResponseEntity<?> loginSolicitud(
            @PathVariable Long solicitudId,
            @RequestBody PinReq req
    ) {
        if (req == null || req.pin() == null || req.pin().isBlank()) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "PIN_REQUIRED",
                    "message", "Ingresá el PIN."
            ));
        }

        try {
            presupuestoService.verificarPinSolicitud(solicitudId, req.pin().trim());
        } catch (IllegalStateException e) {
            String code = e.getMessage();
            int status = switch (code) {
                case "PIN_BLOQUEADO" -> 429;
                case "PIN_EXPIRADO", "PIN_INVALIDO", "PIN_REQUIRED", "PIN_NO_CONFIGURADO" -> 401;
                default -> 401;
            };

            String message = switch (code) {
                case "PIN_BLOQUEADO" -> "PIN bloqueado por demasiados intentos. Probá más tarde.";
                case "PIN_EXPIRADO" -> "El PIN expiró. Solicitá uno nuevo.";
                case "PIN_INVALIDO" -> "El PIN ingresado no es válido.";
                case "PIN_REQUIRED" -> "Ingresá el PIN.";
                case "PIN_NO_CONFIGURADO" -> "La solicitud no tiene PIN configurado.";
                default -> code;
            };

            return ResponseEntity.status(status).body(Map.of(
                    "error", code,
                    "message", message
            ));
        }

        String token = jwtService.createToken(Map.of(
                "type", "PUBLIC_SOLICITUD",
                "solicitudId", solicitudId
        ));

        return ResponseEntity.ok(Map.of(
                "accessToken", token
        ));
    }

    @PostMapping("/orden/{nroOrden}")
    public ResponseEntity<?> loginOrden(
            @PathVariable String nroOrden,
            @RequestBody PinReq req
    ) {
        if (req == null || req.pin() == null || req.pin().isBlank()) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "PIN_REQUIRED",
                    "message", "Ingresá el PIN."
            ));
        }

        Long solicitudId;
        try {
            solicitudId = presupuestoService.getSolicitudIdByNroOrden(nroOrden);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "NOT_FOUND",
                    "message", "No existe una solicitud asociada a esa orden."
            ));
        }

        try {
            presupuestoService.verificarPinSolicitud(solicitudId, req.pin().trim());
        } catch (IllegalStateException e) {
            String code = e.getMessage();
            int status = switch (code) {
                case "PIN_BLOQUEADO" -> 429;
                case "PIN_EXPIRADO", "PIN_INVALIDO", "PIN_REQUIRED", "PIN_NO_CONFIGURADO" -> 401;
                default -> 401;
            };

            String message = switch (code) {
                case "PIN_BLOQUEADO" -> "PIN bloqueado por demasiados intentos. Probá más tarde.";
                case "PIN_EXPIRADO" -> "El PIN expiró. Solicitá uno nuevo.";
                case "PIN_INVALIDO" -> "El PIN ingresado no es válido.";
                case "PIN_REQUIRED" -> "Ingresá el PIN.";
                case "PIN_NO_CONFIGURADO" -> "La orden no tiene un PIN configurado.";
                default -> code;
            };

            return ResponseEntity.status(status).body(Map.of(
                    "error", code,
                    "message", message
            ));
        }

        String token = jwtService.createToken(Map.of(
                "type", "PUBLIC_SOLICITUD",
                "solicitudId", solicitudId,
                "nroOrden", nroOrden
        ));

        return ResponseEntity.ok(Map.of(
                "accessToken", token
        ));
    }

    @PostMapping("/solicitud/{solicitudId}/reenviar-pin")
    public ResponseEntity<?> reenviarPinSolicitud(@PathVariable Long solicitudId) {
        try {
            presupuestoService.reenviarPinSolicitud(solicitudId, PIN_TTL_MINUTES);
            return ResponseEntity.ok(Map.of(
                    "message", "PIN reenviado correctamente."
            ));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "NOT_FOUND",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/orden/{nroOrden}/reenviar-pin")
    public ResponseEntity<?> reenviarPinOrden(@PathVariable String nroOrden) {
        try {
            presupuestoService.reenviarPinOrden(nroOrden, PIN_TTL_MINUTES);

            return ResponseEntity.ok(Map.of(
                    "message", "PIN reenviado correctamente."
            ));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "NOT_FOUND",
                    "message", e.getMessage()
            ));
        }
    }
}