package ar.edu.utn.tfi.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "notificacion", indexes = {
        @Index(name = "idx_notif_nro_orden", columnList = "nro_orden")
})
public class Notificacion {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "orden_id", nullable = false)
    private Long ordenId;

    @Column(name = "nro_orden", nullable = false, length = 30)
    private String nroOrden;

    @Column(name = "canal", nullable = false, length = 20)
    private String canal; // IN_APP | WHATSAPP

    @Column(name = "tipo", nullable = false, length = 40)
    private String tipo;  // LISTO_RETIRAR

    @Column(name = "mensaje", nullable = false, columnDefinition = "text")
    private String mensaje;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado; // PENDIENTE | ENVIADA | ERROR | LEIDA

    @Column(name = "cliente_destino", length = 80)
    private String clienteDestino; // Tel/email opcional

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (estado == null) estado = "PENDIENTE";
    }
}