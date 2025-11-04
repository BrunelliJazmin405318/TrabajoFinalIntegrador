// src/main/java/ar/edu/utn/tfi/domain/PagoManual.java
package ar.edu.utn.tfi.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "pago_manual")
public class PagoManual {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "presupuesto_id")
    private Presupuesto presupuesto;

    @Column(nullable = false, length = 10)
    private String tipo; // SENA | FINAL

    @Column(nullable = false, length = 30)
    private String medio; // EFECTIVO / TRANSFERENCIA / TARJETA / OTRO

    @Column(length = 100)
    private String referencia;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDateTime fechaPago = LocalDateTime.now();

    @Column(length = 80)
    private String usuario;

    @Column(columnDefinition = "text")
    private String nota;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

}