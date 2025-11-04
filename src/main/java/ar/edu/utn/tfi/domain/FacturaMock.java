package ar.edu.utn.tfi.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "factura_mock")
public class FacturaMock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "presupuesto_id")
    private Presupuesto presupuesto;

    @Column(length = 2, nullable = false)
    private String tipo; // A o B

    @Column(length = 20, nullable = false)
    private String numero;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision = LocalDateTime.now();

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(length = 120)
    private String clienteNombre;

    @Column(length = 120)
    private String clienteEmail;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}