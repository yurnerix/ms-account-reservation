package by.yurnerix.msaccountreservation.entity;


import by.yurnerix.msaccountreservation.event.ClientChangedEvent;
import by.yurnerix.msaccountreservation.event.ClientEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "client_event_audit")
public class ClientEventAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false, length = 20)
    private ClientEventType eventType;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    public static ClientEventAudit from(ClientChangedEvent event) {
        ClientEventAudit audit = new ClientEventAudit();

        audit.clientId = event.clientId();
        audit.eventId = event.eventId();
        audit.eventType = event.eventType();
        audit.occurredAt = event.occurredAt();
        audit.processedAt = Instant.now();

        return audit;
    }

}
