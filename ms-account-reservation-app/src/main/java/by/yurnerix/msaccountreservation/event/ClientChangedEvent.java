package by.yurnerix.msaccountreservation.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ClientChangedEvent(UUID eventId, UUID clientId, ClientEventType eventType, Instant occurredAt) {

    public ClientChangedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(clientId, "clientId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static ClientChangedEvent create(UUID clientId, ClientEventType eventType) {
        return new ClientChangedEvent(UUID.randomUUID(), clientId, eventType, Instant.now());
    }
}
