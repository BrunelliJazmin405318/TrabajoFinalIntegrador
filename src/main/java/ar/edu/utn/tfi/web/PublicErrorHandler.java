package ar.edu.utn.tfi.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class PublicErrorHandler {

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> illegalState(IllegalStateException e) {
        String code = e.getMessage() == null ? "ERROR" : e.getMessage();

        return switch (code) {
            case "PIN_REQUERIDO" ->
                    ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", code));

            case "PIN_INVALIDO" ->
                    ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", code));

            case "PIN_EXPIRADO" ->
                    ResponseEntity.status(HttpStatus.GONE).body(Map.of("error", code));

            case "PIN_BLOQUEADO" ->
                    ResponseEntity.status(HttpStatus.LOCKED).body(Map.of("error", code));

            case "PIN_NO_CONFIGURADO" ->
                    ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", code));

            default ->
                    ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", code));
        };
    }
}