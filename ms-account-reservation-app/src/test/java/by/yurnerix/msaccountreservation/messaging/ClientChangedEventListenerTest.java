package by.yurnerix.msaccountreservation.messaging;

import by.yurnerix.msaccountreservation.event.ClientChangedEvent;
import by.yurnerix.msaccountreservation.event.ClientEventType;
import by.yurnerix.msaccountreservation.exception.InvalidClientEventException;
import by.yurnerix.msaccountreservation.service.ClientEventProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientChangedEventListenerTest {

    private static final String TOPIC = "client-changed.v1";

    private static final UUID CLIENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID EVENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final Instant OCCURRED_AT = Instant.parse("2026-11-01T10:00:00Z");

    @Mock
    private ClientEventProcessingService processingService;

    @Mock
    private Acknowledgment acknowledgment;

    private ObjectMapper objectMapper;
    private ClientChangedEventListener listener;
    private ClientChangedEvent event;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        listener = new ClientChangedEventListener(objectMapper, processingService);

        event = new ClientChangedEvent(EVENT_ID, CLIENT_ID, ClientEventType.CREATED, OCCURRED_AT);
    }

    @Test
    void shouldProcessEventBeforeAcknowledgingOffset() throws Exception {
        String payload = objectMapper.writeValueAsString(event);

        when(processingService.process(event))
                .thenReturn(true);

        listener.onMessage(record(CLIENT_ID.toString(), payload), acknowledgment);

        InOrder order = inOrder(processingService, acknowledgment);

        order.verify(processingService).process(event);
        order.verify(acknowledgment).acknowledge();

        verifyNoMoreInteractions(processingService, acknowledgment);
    }

    @Test
    void shouldAcknowledgeDuplicateEvent() throws Exception {
        String payload = objectMapper.writeValueAsString(event);

        when(processingService.process(event))
                .thenReturn(false);

        listener.onMessage(record(CLIENT_ID.toString(), payload), acknowledgment);

        InOrder order = inOrder(processingService, acknowledgment);

        order.verify(processingService).process(event);
        order.verify(acknowledgment).acknowledge();

        verifyNoMoreInteractions(processingService, acknowledgment);
    }

    @Test
    void shouldNotAcknowledgeWhenProcessingFails() throws Exception {
        String payload = objectMapper.writeValueAsString(event);

        DataAccessResourceFailureException failure = new DataAccessResourceFailureException("Database temporarily unavailable");

        when(processingService.process(event))
                .thenThrow(failure);

        DataAccessResourceFailureException actual = assertThrows(DataAccessResourceFailureException.class,
                () -> listener.onMessage(record(CLIENT_ID.toString(), payload), acknowledgment));

        assertSame(failure, actual);

        verify(processingService).process(event);
        verifyNoInteractions(acknowledgment);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "",
            " ",
            "null",
            "{broken-json"
    })
    void shouldRejectInvalidPayloadWithoutAcknowledging(String payload) {
        assertThrows(InvalidClientEventException.class, () -> listener.onMessage(record(CLIENT_ID.toString(), payload), acknowledgment));

        verifyNoInteractions(processingService, acknowledgment);
    }

    @Test
    void shouldRejectEventWhenKeyDoesNotMatchClientId() throws Exception {
        String payload = objectMapper.writeValueAsString(event);

        String wrongKey = "33333333-3333-3333-3333-333333333333";

        assertThrows(InvalidClientEventException.class, () -> listener.onMessage(record(wrongKey, payload), acknowledgment));

        verifyNoInteractions(processingService, acknowledgment);
    }

    @Test
    void shouldRejectEventWithoutEventId() throws Exception {
        ObjectNode payloadNode = objectMapper.valueToTree(event);
        payloadNode.remove("eventId");

        String payload = objectMapper.writeValueAsString(payloadNode);

        InvalidClientEventException exception = assertThrows(InvalidClientEventException.class, () -> listener.onMessage(record(CLIENT_ID.toString(), payload), acknowledgment));

        assertNotNull(exception.getCause());

        verifyNoInteractions(processingService, acknowledgment);
    }

    private ConsumerRecord<String, String> record(String key, String payload) {
        return new ConsumerRecord<>(TOPIC, 0, 15L, key, payload);
    }
}
