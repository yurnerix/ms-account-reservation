package by.yurnerix.msaccountreservation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class ProcessedClientEventId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;
}
