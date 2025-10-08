package ar.edu.utn.tfi.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "demora_motivo")
@Getter @Setter
public class DemoraMotivo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String codigo;       // p.ej. FALTA_REPUESTO

    private String descripcion;  // opcional
}
