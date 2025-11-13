package ar.edu.utn.tfi.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SolicitudCreateDTO(

        @NotBlank(message = "El nombre del cliente es obligatorio.")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres.")
        String clienteNombre,

        @NotBlank(message = "El teléfono es obligatorio.")
        @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres.")
        @Pattern(
                regexp = "^[0-9+()\\s-]{6,30}$",
                message = "El teléfono contiene caracteres inválidos."
        )
        String clienteTelefono,

        @NotBlank(message = "El email es obligatorio.")
        @Email(message = "El email no tiene un formato válido.")
        @Size(max = 120, message = "El email no puede superar los 120 caracteres.")
        String clienteEmail,

        @NotBlank(message = "El tipo de unidad es obligatorio.")
        @Size(max = 40, message = "El tipo de unidad no puede superar los 40 caracteres.")
        String tipoUnidad,   // MOTOR | TAPA

        @NotBlank(message = "La marca es obligatoria.")
        @Size(max = 60, message = "La marca no puede superar los 60 caracteres.")
        String marca,

        @NotBlank(message = "El modelo es obligatorio.")
        @Size(max = 60, message = "El modelo no puede superar los 60 caracteres.")
        String modelo,

        @Size(max = 60, message = "El número de motor no puede superar los 60 caracteres.")
        String nroMotor,

        @NotBlank(message = "La descripción del trabajo es obligatoria.")
        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres.")
        String descripcion
) {
}