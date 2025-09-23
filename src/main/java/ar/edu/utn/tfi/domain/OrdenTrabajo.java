package ar.edu.utn.tfi.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "orden_trabajo", indexes = {
        @Index(name = "idx_orden_nro", columnList = "nro_orden", unique = true)
})
public class OrdenTrabajo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nro_orden", nullable = false, unique = true, length = 30)
    private String nroOrden;

    @Column(name = "unidad_id", nullable = false)
    private Long unidadId;

    @Column(name = "estado_actual", nullable = false, length = 40)
    private String estadoActual;

    @Column(name = "garantia_desde")
    private LocalDate garantiaDesde;

    @Column(name = "garantia_hasta")
    private LocalDate garantiaHasta;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;
}
