// src/main/java/ar/edu/utn/tfi/domain/Notificacion.java
package ar.edu.utn.tfi.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;        // p.ej. LISTO_RETIRAR
    private String title;       // NOT NULL en DB
    private String message;     // NOT NULL en DB

    @Column(name = "orden_id")
    private Long ordenId;

    @Column(name = "solicitud_id")
    private Long solicitudId;

    @Column(name = "cliente_email")
    private String clienteEmail;

    @Column(name = "channel")
    private String canal;       // IN_APP / WHATSAPP / EMAIL

    @Column(name = "sent")
    private Boolean sent;       // default false

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt; // NOT NULL

    @Column(name = "metadata_json")
    private String metadataJson;

    // Campos extra
    @Column(name = "nro_orden")
    private String nroOrden;

    @Column(name = "estado")
    private String estado;

    @Column(name = "cliente_destino")
    private String clienteDestino;

    // ---------- Defaults antes de insertar ----------
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (sent == null) {
            sent = false;
        }
        // Por seguridad, si no viene título/mensaje (evita violar NOT NULL)
        if (title == null) {
            title = "Notificación";
        }
        if (message == null) {
            message = "";
        }
    }
}
