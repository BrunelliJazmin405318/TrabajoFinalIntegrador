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

    // ───────────── Campos de SEÑA (Checkout API / manual) ─────────────

    @Column(name = "sena_monto", precision = 12, scale = 2)
    private BigDecimal senaMonto;

    // PENDIENTE | ACREDITADA | CANCELADA
    @Column(name = "sena_estado")
    private String senaEstado;

    @Column(name = "sena_payment_id")
    private String senaPaymentId;

    @Column(name = "sena_payment_status")
    private String senaPaymentStatus;

    @Column(name = "sena_paid_at")
    private LocalDateTime senaPaidAt;

    // ───────────── Campos de PAGO FINAL (manual o futuro online) ─────────────

    @Column(name = "final_monto", precision = 12, scale = 2)
    private BigDecimal finalMonto;

    // PENDIENTE | ACREDITADO | CANCELADO
    @Column(name = "final_estado")
    private String finalEstado;

    // Referencia/comprobante/ID de operación (si aplica)
    @Column(name = "final_payment_id")
    private String finalPaymentId;

    // Estado del pago: approved/authorized/rejected/etc. (si aplica)
    @Column(name = "final_payment_status")
    private String finalPaymentStatus;

    // Fecha/hora de acreditación
    @Column(name = "final_paid_at")
    private LocalDateTime finalPaidAt;

    @Column(name = "pieza_tipo", length = 20)
    private String piezaTipo; //

    // ─────────────────────────────────────────────────────────────────────────

    @PrePersist
    void prePersist() {
        if (creadaEn == null) creadaEn = LocalDateTime.now();
        if (estado == null) estado = "PENDIENTE";
        if (total == null) total = BigDecimal.ZERO;

        // Defaults “suaves” por si los usás en validaciones
        if (senaEstado == null || senaEstado.isBlank()) senaEstado = "PENDIENTE";
        if (finalEstado == null || finalEstado.isBlank()) finalEstado = "PENDIENTE";
    }
}