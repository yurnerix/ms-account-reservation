package by.yurnerix.msaccountreservation.service;

import by.yurnerix.msaccountreservation.entity.ClientEventOutbox;
import by.yurnerix.msaccountreservation.event.ClientChangedEvent;
import by.yurnerix.msaccountreservation.event.ClientEventType;
import by.yurnerix.msaccountreservation.repository.ClientEventOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientEventOutboxWriter {

    private final ClientEventOutboxRepository outboxRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void write(UUID clientId, ClientEventType eventType) {
        ClientChangedEvent event = ClientChangedEvent.create(clientId, eventType);

        ClientEventOutbox outbox = ClientEventOutbox.from(event);

        outboxRepository.saveAndFlush(outbox);
    }
}
