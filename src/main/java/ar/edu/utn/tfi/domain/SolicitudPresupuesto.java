package ar.edu.utn.tfi.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "presupuesto_solicitud")
public class SolicitudPresupuesto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="cliente_nombre", nullable=false)
    private String clienteNombre;

    @Column(name="cliente_telefono")
    private String clienteTelefono;

    @Column(name="cliente_email")
    private String clienteEmail;

    @Column(name="tipo_unidad", nullable=false)
    private String tipoUnidad; // MOTOR | TAPA

    private String marca;
    private String modelo;

    @Column(name="nro_motor")
    private String nroMotor;

    private String descripcion;

    @Column(nullable=false)
    private String estado = "PENDIENTE"; // PENDIENTE | APROBADO | RECHAZADO

    @Column(name="creada_en", nullable=false)
    private LocalDateTime creadaEn;

    @Column(name="decision_usuario")
    private String decisionUsuario;

    @Column(name="decision_fecha")
    private LocalDateTime decisionFecha;

    @Column(name="decision_motivo")
    private String decisionMotivo;

    @Column(name = "tipo_consulta", length = 20)
    private String tipoConsulta;   // COTIZACION | DIAGNOSTICO

    @PrePersist
    void pre() {
        if (creadaEn == null) creadaEn = LocalDateTime.now();
        if (estado == null) estado = "PENDIENTE";
    }
}
