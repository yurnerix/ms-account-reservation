package by.yurnerix.msaccountreservation.integration;

import by.yurnerix.msaccountreservation.entity.ClientEventAudit;
import by.yurnerix.msaccountreservation.event.ClientChangedEvent;
import by.yurnerix.msaccountreservation.event.ClientEventType;
import by.yurnerix.msaccountreservation.repository.ClientEventAuditRepository;
import by.yurnerix.msaccountreservation.repository.ProcessedClientEventRepository;
import by.yurnerix.msaccountreservation.service.ClientEventProcessingService;
import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.junit5.api.DBRider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DBRider
@DataSet(
        value = "datasets/client/active-client.yml",
        cleanBefore = true,
        cleanAfter = true,
        disableConstraints = true,
        skipCleaningFor = {
                "account_status",
                "databasechangelog",
                "databasechangeloglock"
        }
)
@Isolated("Database Rider очищает общую тестовую БД")
class ClientEventProcessingIntegrationTest extends AbstractMockedCurrencyIntegrationTest {

    private static final UUID CLIENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final Instant OCCURRED_AT = Instant.parse("2026-10-31T10:00:00Z");

    @Autowired
    private ClientEventProcessingService processingService;

    @Autowired
    private ProcessedClientEventRepository processedEventRepository;

    @Autowired
    private ClientEventAuditRepository auditRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void shouldProcessNewEvent() {
        ClientChangedEvent event = createEvent(ClientEventType.CREATED);

        boolean processed = processingService.process(event);

        assertTrue(processed);
        assertEquals(1L, processedEventRepository.count());

        List<ClientEventAudit> records = auditRepository.findAll();

        assertEquals(1, records.size());

        ClientEventAudit audit = records.getFirst();

        assertAll(
                () -> assertEquals(event.clientId(), audit.getClientId()),
                () -> assertEquals(event.eventId(), audit.getEventId()),
                () -> assertEquals(event.eventType(), audit.getEventType()),
                () -> assertEquals(event.occurredAt(), audit.getOccurredAt()),
                () -> assertNotNull(audit.getProcessedAt())
        );
    }

    @Test
    void shouldSkipDuplicateEvent() {
        ClientChangedEvent event = createEvent(ClientEventType.CREATED);

        assertTrue(processingService.process(event));
        assertFalse(processingService.process(event));

        assertEquals(1L, processedEventRepository.count());
        assertEquals(1L, auditRepository.count());
    }

    @Test
    void shouldSkipOldDuplicateAfterAnotherEventWasProcessed() {
        ClientChangedEvent first = createEvent(ClientEventType.CREATED);

        ClientChangedEvent second = createEvent(ClientEventType.UPDATED);

        assertTrue(processingService.process(first));
        assertTrue(processingService.process(second));
        assertFalse(processingService.process(first));

        assertEquals(2L, processedEventRepository.count());
        assertEquals(2L, auditRepository.count());
    }

    @Test
    void shouldRollbackBothRecordsAndAllowRetry() {
        ClientChangedEvent event = createEvent(ClientEventType.CREATED);

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        RuntimeException failure = new RuntimeException("Test failure after event processing");

        RuntimeException actualException = assertThrows(RuntimeException.class, () -> transactionTemplate.executeWithoutResult(status -> {
                    assertTrue(processingService.process(event));

                    assertEquals(1L, processedEventRepository.count());
                    assertEquals(1L, auditRepository.count());

                    throw failure;
                })
        );

        assertSame(failure, actualException);

        assertEquals(0L, processedEventRepository.count());
        assertEquals(0L, auditRepository.count());

        assertTrue(processingService.process(event), "После отката повторная обработка должна быть разрешена");

        assertEquals(1L, processedEventRepository.count());
        assertEquals(1L, auditRepository.count());
    }

    private ClientChangedEvent createEvent(ClientEventType eventType) {
        return new ClientChangedEvent(UUID.randomUUID(), CLIENT_ID, eventType, OCCURRED_AT);
    }
}
