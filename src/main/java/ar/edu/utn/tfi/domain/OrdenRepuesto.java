package ar.edu.utn.tfi.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "orden_repuesto")
public class OrdenRepuesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Igual que OrdenEtapaHistorial: FK simple
    @Column(name = "orden_id", nullable = false)
    private Long ordenId;

    @Column(nullable = false, length = 200)
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidad = BigDecimal.ONE;

    @Column(name = "precio_unit", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnit;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;   // ⬅️ NUEVO

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "created_by", length = 80)
    private String createdBy;
}