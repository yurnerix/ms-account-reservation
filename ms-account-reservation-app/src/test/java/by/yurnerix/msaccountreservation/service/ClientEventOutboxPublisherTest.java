package by.yurnerix.msaccountreservation.service;

import by.yurnerix.msaccountreservation.config.ClientKafkaProperties;
import by.yurnerix.msaccountreservation.entity.ClientEventOutbox;
import by.yurnerix.msaccountreservation.event.ClientChangedEvent;
import by.yurnerix.msaccountreservation.event.ClientEventType;
import by.yurnerix.msaccountreservation.repository.ClientEventOutboxRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientEventOutboxPublisherTest {

    private static final String TOPIC = "client-changed.v1";

    private static final UUID CLIENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID EVENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final Instant OCCURRED_AT = Instant.parse("2026-10-31T10:00:00Z");

    @Mock
    private ClientEventOutboxRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private SendResult<String, String> sendResult;

    private ClientKafkaProperties properties;
    private ObjectMapper objectMapper;
    private ClientEventOutboxPublisher publisher;
    private ClientEventOutbox outbox;

    @BeforeEach
    void setUp() {
        properties = new ClientKafkaProperties();
        properties.setClientEventsTopic(TOPIC);

        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        publisher = new ClientEventOutboxPublisher(outboxRepository, kafkaTemplate, objectMapper, properties);

        ClientChangedEvent event = new ClientChangedEvent(EVENT_ID, CLIENT_ID, ClientEventType.CREATED, OCCURRED_AT);

        outbox = ClientEventOutbox.from(event);
    }

    @Test
    void shouldReturnFalseWhenNoPendingEvents() {
        when(outboxRepository.findNextForPublishing())
                .thenReturn(Optional.empty());

        boolean result = publisher.publishNext();

        assertFalse(result);

        verifyNoInteractions(kafkaTemplate);
        verify(outboxRepository, never()).flush();
    }

    @Test
    void shouldPublishEventWithClientKeyAndMarkItPublished() throws Exception {
        when(outboxRepository.findNextForPublishing())
                .thenReturn(Optional.of(outbox));

        when(sendResult.getRecordMetadata())
                .thenReturn(mock(RecordMetadata.class));

        when(kafkaTemplate.send(eq(TOPIC), eq(CLIENT_ID.toString()), anyString()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        boolean result = publisher.publishNext();

        assertTrue(result);

        assertAll(
                () -> assertNotNull(outbox.getPublishedAt()),
                () -> assertEquals(1, outbox.getAttempts()),
                () -> assertNull(outbox.getLastError())
        );

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

        verify(kafkaTemplate).send(eq(TOPIC), eq(CLIENT_ID.toString()), payloadCaptor.capture());

        JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());

        assertAll(
                () -> assertEquals(EVENT_ID.toString(), payload.get("eventId").asText()),
                () -> assertEquals(CLIENT_ID.toString(), payload.get("clientId").asText()),
                () -> assertEquals("CREATED", payload.get("eventType").asText()),
                () -> assertEquals(OCCURRED_AT.toString(), payload.get("occurredAt").asText())
        );

        verify(outboxRepository).flush();
    }

    @Test
    void shouldScheduleRetryWhenKafkaSendFails() {
        when(outboxRepository.findNextForPublishing())
                .thenReturn(Optional.of(outbox));

        when(kafkaTemplate.send(eq(TOPIC), eq(CLIENT_ID.toString()), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new KafkaException("Kafka unavailable")));

        Instant earliestRetry = Instant.now()
                .plusMillis(properties.getOutboxRetryDelayMs());

        boolean result = publisher.publishNext();

        assertFalse(result);

        assertAll(
                () -> assertNull(outbox.getPublishedAt()),
                () -> assertEquals(1, outbox.getAttempts()),
                () -> assertNotNull(outbox.getLastError()),
                () -> assertTrue(outbox.getLastError().contains("Kafka unavailable")),
                () -> assertFalse(outbox.getNextAttemptAt().isBefore(earliestRetry))
        );

        verify(outboxRepository).flush();
    }

    @Test
    void shouldScheduleRetryWhenSendConfirmationTimesOut() {
        properties.setOutboxSendTimeoutMs(1);

        when(outboxRepository.findNextForPublishing())
                .thenReturn(Optional.of(outbox));

        CompletableFuture<SendResult<String, String>> pendingSend = new CompletableFuture<>();

        when(kafkaTemplate.send(eq(TOPIC), eq(CLIENT_ID.toString()), anyString()))
                .thenReturn(pendingSend);

        boolean result = publisher.publishNext();

        assertFalse(result);

        assertAll(
                () -> assertNull(outbox.getPublishedAt()),
                () -> assertEquals(1, outbox.getAttempts()),
                () -> assertNotNull(outbox.getLastError()),
                () -> assertTrue(outbox.getLastError().contains("TimeoutException"))
        );

        verify(outboxRepository).flush();
    }

    @Test
    void shouldKeepSameEventIdWhenRetryingPublication() throws Exception {
        when(outboxRepository.findNextForPublishing())
                .thenReturn(Optional.of(outbox));

        when(sendResult.getRecordMetadata())
                .thenReturn(mock(RecordMetadata.class));

        CompletableFuture<SendResult<String, String>> failedSend = CompletableFuture.failedFuture(new KafkaException("Temporary Kafka failure"));

        CompletableFuture<SendResult<String, String>> successfulSend = CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(eq(TOPIC), eq(CLIENT_ID.toString()), anyString()))
                .thenReturn(failedSend)
                .thenReturn(successfulSend);

        assertFalse(publisher.publishNext());
        assertTrue(publisher.publishNext());

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

        verify(kafkaTemplate, times(2)).send(eq(TOPIC), eq(CLIENT_ID.toString()), payloadCaptor.capture());

        List<String> payloads = payloadCaptor.getAllValues();

        JsonNode firstPayload = objectMapper.readTree(payloads.get(0));
        JsonNode secondPayload = objectMapper.readTree(payloads.get(1));

        assertAll(
                () -> assertEquals(firstPayload, secondPayload),
                () -> assertEquals(EVENT_ID.toString(), secondPayload.get("eventId").asText()),
                () -> assertEquals(2, outbox.getAttempts()),
                () -> assertNotNull(outbox.getPublishedAt()),
                () -> assertNull(outbox.getLastError())
        );

        verify(outboxRepository, times(2)).flush();
    }

    @Test
    void shouldPropagateDatabaseFailureAfterKafkaAcknowledgement() {
        when(outboxRepository.findNextForPublishing())
                .thenReturn(Optional.of(outbox));

        when(kafkaTemplate.send(eq(TOPIC), eq(CLIENT_ID.toString()), anyString()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        DataAccessResourceFailureException failure = new DataAccessResourceFailureException("Cannot persist publication result");

        doThrow(failure)
                .when(outboxRepository)
                .flush();

        DataAccessResourceFailureException actualException = assertThrows(DataAccessResourceFailureException.class, () -> publisher.publishNext());

        assertSame(failure, actualException);

        verify(outboxRepository).flush();

        verify(kafkaTemplate).send(eq(TOPIC), eq(CLIENT_ID.toString()), anyString());
    }
}
