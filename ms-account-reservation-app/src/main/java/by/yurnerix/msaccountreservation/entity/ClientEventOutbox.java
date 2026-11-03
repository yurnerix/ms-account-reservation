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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "client_event_outbox", uniqueConstraints = {
                @UniqueConstraint(name = "uk_client_event_outbox_event_id", columnNames = "event_id")
        }
)
public class ClientEventOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false, length = 20)
    private ClientEventType eventType;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    public static ClientEventOutbox from(ClientChangedEvent event) {
        ClientEventOutbox outbox = new ClientEventOutbox();

        outbox.eventId = event.eventId();
        outbox.clientId = event.clientId();
        outbox.eventType = event.eventType();
        outbox.occurredAt = event.occurredAt();
        outbox.nextAttemptAt = event.occurredAt();

        return outbox;
    }

    public ClientChangedEvent toEvent() {
        return new ClientChangedEvent(eventId, clientId, eventType, occurredAt);
    }

    // подтверждённая отправка
    public void markPublished(Instant publishedAt) {
        this.publishedAt = Objects.requireNonNull(publishedAt, "publishedAt must not be null");

        this.attempts++;
        this.lastError = null;
    }

    // неудачная попытка
    public void markFailed(Instant nextAttemptAt, String error) {
        this.nextAttemptAt = Objects.requireNonNull(nextAttemptAt, "nextAttemptAt must not be null");

        this.lastError = Objects.requireNonNull(error, "error must not be null");

        this.attempts++;
    }
}
