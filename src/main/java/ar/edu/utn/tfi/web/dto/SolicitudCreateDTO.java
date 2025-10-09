package ar.edu.utn.tfi.web.dto;

public record SolicitudCreateDTO(String clienteNombre,
                                 String clienteTelefono,
                                 String clienteEmail,
                                 String tipoUnidad,   // MOTOR | TAPA
                                 String marca,
                                 String modelo,
                                 String nroMotor,
                                 String descripcion) {
}
