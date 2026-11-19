package by.yurnerix.msaccountreservation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "processed_client_event")
public class ProcessedClientEvent {

    @EmbeddedId
    private ProcessedClientEventId id;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;
}
