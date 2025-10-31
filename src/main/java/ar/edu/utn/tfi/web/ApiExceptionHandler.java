package ar.edu.utn.tfi.web;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    // 404 – cuando no existe la entidad
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "NOT_FOUND");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // 409 – cuando la operación no es válida por el estado actual (reglas de negocio)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(IllegalStateException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "CONFLICT");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // 400 – validaciones de Spring (@Valid) o argumentos inválidos
    @ExceptionHandler({MethodArgumentNotValidException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "BAD_REQUEST");
        body.put("message", ex.getMessage() != null ? ex.getMessage() : "Datos inválidos");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 502 – error al comunicarse con un servicio externo (por ejemplo, Mercado Pago)
    @ExceptionHandler(com.mercadopago.exceptions.MPException.class)
    public ResponseEntity<Map<String, Object>> handleMercadoPago(com.mercadopago.exceptions.MPException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "MERCADOPAGO_ERROR");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }

    // 500 – catch-all (errores no esperados)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "INTERNAL_ERROR");
        body.put("message", ex.getMessage() != null ? ex.getMessage() : "Error interno inesperado");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}