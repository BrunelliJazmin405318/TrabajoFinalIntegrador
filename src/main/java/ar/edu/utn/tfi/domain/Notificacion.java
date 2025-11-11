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
@Table(name = "notification") // << usa la tabla que ya existe
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String type;                 // ej: MOTOR_LISTO

    @Column(nullable = false, length = 150)
    private String title;                // título breve

    @Column(nullable = false, length = 1000)
    private String message;              // texto del aviso

    @Column(name = "orden_id")
    private Long ordenId;                // ref OT (opcional)

    @Column(name = "solicitud_id")
    private Long solicitudId;            // ref Solicitud (opcional)

    @Column(name = "cliente_email", length = 200)
    private String clienteEmail;         // destinatario

    @Column(nullable = false, length = 30)
    private String channel;              // WEB | WHATSAPP | EMAIL

    @Column(nullable = false)
    private boolean sent = false;        // si fue “enviado”

    @Column(name = "read_at")
    private LocalDateTime readAt;        // cuándo lo leyó (null si no)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;         // extra (opcional)

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (channel == null || channel.isBlank()) channel = "WEB";
    }
}
