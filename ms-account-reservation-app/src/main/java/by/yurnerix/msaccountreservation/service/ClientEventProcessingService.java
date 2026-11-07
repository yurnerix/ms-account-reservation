package by.yurnerix.msaccountreservation.service;

import by.yurnerix.msaccountreservation.entity.ClientEventAudit;
import by.yurnerix.msaccountreservation.event.ClientChangedEvent;
import by.yurnerix.msaccountreservation.repository.ClientEventAuditRepository;
import by.yurnerix.msaccountreservation.repository.ProcessedClientEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientEventProcessingService {

    private final ProcessedClientEventRepository processedEventRepository;
    private final ClientEventAuditRepository auditRepository;

    @Transactional
    public boolean process(ClientChangedEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        int inserted = processedEventRepository.insertIfAbsent(event.clientId(), event.eventId());

        if (inserted == 0) {
            log.debug("Duplicate client event skipped: clientId={}, eventId={}", event.clientId(), event.eventId());

            return false;
        }

        ClientEventAudit audit = ClientEventAudit.from(event);

        auditRepository.saveAndFlush(audit);

        return true;
    }

}
