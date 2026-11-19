package by.yurnerix.msaccountreservation.integration;

import by.yurnerix.msaccountreservation.entity.Client;
import by.yurnerix.msaccountreservation.entity.ClientEventOutbox;
import by.yurnerix.msaccountreservation.entity.ClientStatus;
import by.yurnerix.msaccountreservation.event.ClientEventType;
import by.yurnerix.msaccountreservation.generated.dto.ClientResponseDto;
import by.yurnerix.msaccountreservation.generated.dto.CreateClientRequestDto;
import by.yurnerix.msaccountreservation.generated.dto.UpdateClientRequestDto;
import by.yurnerix.msaccountreservation.repository.ClientEventOutboxRepository;
import by.yurnerix.msaccountreservation.repository.ClientRepository;
import by.yurnerix.msaccountreservation.service.ClientEventOutboxWriter;
import by.yurnerix.msaccountreservation.service.ClientService;
import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.junit5.api.DBRider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DBRider
@DataSet(value = "datasets/client/active-client.yml",
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
class ClientOutboxIntegrationTest extends AbstractMockedCurrencyIntegrationTest {

    private static final UUID EXISTING_CLIENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final long NEW_CLIENT_MDM_ID = 20261031099L;

    @Autowired
    private ClientService clientService;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientEventOutboxRepository outboxRepository;

    @Autowired
    private ClientEventOutboxWriter outboxWriter;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    void createClientShouldCommitClientAndOutboxEvent() {
        ClientResponseDto response = clientService.createClient(createValidRequest());

        Client savedClient = clientRepository
                .findById(response.getId())
                .orElseThrow();

        List<ClientEventOutbox> events = outboxRepository.findAll();

        assertEquals(1, events.size());

        ClientEventOutbox event = events.getFirst();

        assertAll(
                () -> assertEquals(Long.valueOf(NEW_CLIENT_MDM_ID), savedClient.getMdmId()),
                () -> assertEquals(ClientStatus.ACTIVE, savedClient.getStatus()),
                () -> assertNotNull(event.getId()),
                () -> assertNotNull(event.getEventId()),
                () -> assertEquals(savedClient.getId(), event.getClientId()),
                () -> assertEquals(ClientEventType.CREATED, event.getEventType()),
                () -> assertNotNull(event.getOccurredAt()),
                () -> assertNotNull(event.getNextAttemptAt()),
                () -> assertNull(event.getPublishedAt()),
                () -> assertNull(event.getLastError()),
                () -> assertEquals(0, event.getAttempts())
        );
    }

    @Test
    void createClientShouldRollbackClientAndOutboxTogether() {
        RuntimeException failure = new RuntimeException("Test failure after client creation");

        RuntimeException actualException = assertThrows(RuntimeException.class, () -> transactionTemplate.executeWithoutResult(status -> {
                    clientService.createClient(createValidRequest());

                    assertTrue(clientRepository.existsByMdmId(NEW_CLIENT_MDM_ID));
                    assertEquals(1L, outboxRepository.count());

                    throw failure;
                })
        );

        assertSame(failure, actualException);

        assertFalse(clientRepository.existsByMdmId(NEW_CLIENT_MDM_ID), "Созданный клиент должен откатиться");

        assertEquals(0L, outboxRepository.count(), "Событие CREATED должно откатиться вместе с клиентом");
    }

    @Test
    void updateClientShouldRollbackChangesAndOutboxTogether() {
        UpdateClientRequestDto request = new UpdateClientRequestDto();
        request.setFirstName("Иван");
        request.setLastName("Изменённый");
        request.setMiddleName("Сергеевич");

        RuntimeException failure = new RuntimeException("Test failure after client update");

        RuntimeException actualException = assertThrows(RuntimeException.class, () -> transactionTemplate.executeWithoutResult(status -> {
                    clientService.updateClient(EXISTING_CLIENT_ID, request);

                    Client updatedClient = clientRepository
                            .findById(EXISTING_CLIENT_ID)
                            .orElseThrow();

                    assertEquals("Изменённый", updatedClient.getLastName());
                    assertEquals(1L, outboxRepository.count());

                    throw failure;
                })
        );

        assertSame(failure, actualException);

        Client restoredClient = clientRepository
                .findById(EXISTING_CLIENT_ID)
                .orElseThrow();

        assertEquals("Петров", restoredClient.getLastName(), "После отката должна сохраниться фамилия из DataSet");

        assertEquals(0L, outboxRepository.count(), "Событие UPDATED должно откатиться вместе с изменением клиента");
    }

    @Test
    void deleteClientShouldRollbackStatusAndOutboxTogether() {
        RuntimeException failure = new RuntimeException("Test failure after client deletion");

        RuntimeException actualException = assertThrows(
                RuntimeException.class, () -> transactionTemplate.executeWithoutResult(status -> {
                    clientService.deleteClient(EXISTING_CLIENT_ID);

                    Client deletedClient = clientRepository
                            .findById(EXISTING_CLIENT_ID)
                            .orElseThrow();

                    assertEquals(ClientStatus.DELETED, deletedClient.getStatus());
                    assertEquals(1L, outboxRepository.count());

                    throw failure;
                })
        );

        assertSame(failure, actualException);

        Client restoredClient = clientRepository
                .findById(EXISTING_CLIENT_ID)
                .orElseThrow();

        assertEquals(ClientStatus.ACTIVE, restoredClient.getStatus(), "После отката клиент должен остаться активным");

        assertEquals(0L, outboxRepository.count(), "Событие DELETED должно откатиться вместе со статусом клиента");
    }

    @Test
    void outboxWriterShouldRejectCallWithoutTransaction() {
        assertThrows(IllegalTransactionStateException.class, () -> outboxWriter.write(EXISTING_CLIENT_ID, ClientEventType.UPDATED));

        assertEquals(0L, outboxRepository.count());
    }

    private CreateClientRequestDto createValidRequest() {
        CreateClientRequestDto request = new CreateClientRequestDto();

        request.setMdmId(NEW_CLIENT_MDM_ID);
        request.setFirstName("Пётр");
        request.setLastName("Тестовый");
        request.setMiddleName("Андреевич");
        request.setCitizenship("Россия");
        request.setClientType("INDIVIDUAL");
        request.setDocumentNumber("999099");
        request.setDocumentSeries("9999");
        request.setDocumentType("PASSPORT");

        return request;
    }

    // событие доступно после отметки предыдущего
    @Test
    void shouldSelectNextEventAfterPreviousMarkedPublished() {
        ClientResponseDto createdClient = clientService.createClient(createValidRequest());

        clientService.deleteClient(createdClient.getId());

        transactionTemplate.executeWithoutResult(status -> {
            ClientEventOutbox first = outboxRepository
                    .findNextForPublishing()
                    .orElseThrow();

            assertEquals(createdClient.getId(), first.getClientId());
            assertEquals(ClientEventType.CREATED, first.getEventType());

            first.markPublished(Instant.now());
            outboxRepository.flush();

            ClientEventOutbox second = outboxRepository
                    .findNextForPublishing()
                    .orElseThrow();

            assertEquals(createdClient.getId(), second.getClientId());
            assertEquals(ClientEventType.DELETED, second.getEventType());
            assertTrue(first.getId() < second.getId());
            assertNotEquals(first.getEventId(), second.getEventId());

            second.markPublished(Instant.now());
            outboxRepository.flush();

            assertTrue(outboxRepository.findNextForPublishing().isEmpty());
        });
    }

    // ожидание повторной попытки блокирует позднее событие того же клиента
    @Test
    void shouldNotSelectLaterEventWhilePreviousWaitsForRetry() {
        ClientResponseDto createdClient = clientService.createClient(createValidRequest());

        clientService.deleteClient(createdClient.getId());

        transactionTemplate.executeWithoutResult(status -> {
            ClientEventOutbox first = outboxRepository
                    .findNextForPublishing()
                    .orElseThrow();

            assertEquals(ClientEventType.CREATED, first.getEventType());

            first.markFailed(Instant.now().plusSeconds(3600), "Test Kafka connection failure");

            outboxRepository.flush();

            assertNull(first.getPublishedAt());
            assertEquals(1, first.getAttempts());

            assertTrue(outboxRepository.findNextForPublishing().isEmpty(), "DELETED не должно обгонять CREATED, ожидающее повторной отправки");

            assertEquals(2L, outboxRepository.count());
        });
    }
}
