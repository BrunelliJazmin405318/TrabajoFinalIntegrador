package ar.edu.utn.tfi.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "servicio_tarifa",
        uniqueConstraints = @UniqueConstraint(columnNames = {"nombre_servicio","vehiculo_tipo"}))
public class ServicioTarifa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="nombre_servicio", nullable=false, length=80)
    private String nombreServicio;

    @Column(name="vehiculo_tipo", nullable=false, length=16)
    private String vehiculoTipo; // CONVENCIONAL | IMPORTADO

    @Column(nullable=false, precision = 12, scale = 2)
    private BigDecimal precio;
}

