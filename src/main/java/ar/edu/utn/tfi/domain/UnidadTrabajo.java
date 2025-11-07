package ar.edu.utn.tfi.domain;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "unidad_trabajo")
public class UnidadTrabajo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación con cliente (la FK ya existe en la tabla)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    // MOTOR | TAPA (en DB hay un CHECK), lo dejamos como String para no romper nada
    @Column(name = "tipo", length = 16, nullable = false)
    private String tipo;

    @Column(name = "marca", length = 80)
    private String marca;

    @Column(name = "modelo", length = 80)
    private String modelo;

    @Column(name = "nro_motor", length = 80)
    private String nroMotor;
}