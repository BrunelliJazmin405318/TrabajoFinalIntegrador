package ar.edu.utn.tfi.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "mp_event_log", uniqueConstraints = @UniqueConstraint(columnNames = "request_id"))
public class MpEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id")
    private String requestId;

    @Column(name = "topic")
    private String topic;

    @Column(name = "data_id")
    private String dataId;

    @Column(name = "received_at")
    private LocalDateTime receivedAt = LocalDateTime.now();

    @Column(name = "processed")
    private Boolean processed = Boolean.FALSE;

    @Column(name = "error_msg")
    private String errorMsg;

    public MpEventLog(String requestId, String topic, String dataId) {
        this.requestId = requestId;
        this.topic = topic;
        this.dataId = dataId;
    }
}
