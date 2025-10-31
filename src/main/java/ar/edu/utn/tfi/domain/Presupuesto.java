package ar.edu.utn.tfi.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "presupuesto")
public class Presupuesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "solicitud_id", nullable = false)
    private Long solicitudId;

    @Column(name = "cliente_nombre", nullable = false)
    private String clienteNombre;

    @Column(name = "cliente_email")
    private String clienteEmail;

    @Column(name = "vehiculo_tipo", nullable = false) // CONVENCIONAL | IMPORTADO
    private String vehiculoTipo;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "estado", nullable = false) // PENDIENTE | APROBADO | RECHAZADO
    private String estado;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    @Column(name = "decision_usuario")
    private String decisionUsuario;

    @Column(name = "decision_fecha")
    private LocalDateTime decisionFecha;

    @Column(name = "decision_motivo")
    private String decisionMotivo;

    // Campos para manejar la seña (Mercado Pago)
    @Column(name = "sena_preference_id")
    private String senaPreferenceId;

    @Column(name = "sena_monto", precision = 12, scale = 2)
    private BigDecimal senaMonto;

    @Column(name = "sena_estado")
    private String senaEstado;

    @Column(name = "sena_init_point")
    private String senaInitPoint;



    @PrePersist
    void prePersist() {
        if (creadaEn == null) creadaEn = LocalDateTime.now();
        if (estado == null) estado = "PENDIENTE";
        if (total == null) total = BigDecimal.ZERO;
    }
}
