package ar.edu.utn.tfi.domain;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "orden_etapa_historial")
public class OrdenEtapaHistorial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "orden_id", nullable = false)
    private Long ordenId;

    @Column(name = "etapa_codigo", nullable = false, length = 40)
    private String etapaCodigo;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "observacion")
    private String observacion;

    @Column(name = "demora_motivo_id")
    private Long demoraMotivoId;

    @Column(name = "usuario", length = 80)
    private String usuario;
}
