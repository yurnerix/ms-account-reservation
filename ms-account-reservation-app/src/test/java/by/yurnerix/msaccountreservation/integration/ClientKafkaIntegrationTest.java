package by.yurnerix.msaccountreservation.integration;

import by.yurnerix.msaccountreservation.entity.ClientEventOutbox;
import by.yurnerix.msaccountreservation.event.ClientChangedEvent;
import by.yurnerix.msaccountreservation.event.ClientEventType;
import by.yurnerix.msaccountreservation.repository.ClientEventOutboxRepository;
import by.yurnerix.msaccountreservation.service.ClientEventOutboxPublisher;
import by.yurnerix.msaccountreservation.service.ClientEventOutboxScheduler;
import by.yurnerix.msaccountreservation.service.ClientEventProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.junit5.api.DBRider;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DBRider
@MockitoSpyBean(name = "kafkaTemplate", types = KafkaTemplate.class)
@Isolated("Kafka consumer работает с общей тестовой PostgreSQL")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ClientKafkaIntegrationTest extends AbstractMockedCurrencyIntegrationTest {

    private static final String TOPIC = "client-changed-it-" + UUID.randomUUID();

    private static final String DLT_TOPIC = TOPIC + ".DLT";

    private static final String GROUP_ID = "client-events-it-" + UUID.randomUUID();

    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(30);

    private static final Instant OCCURRED_AT = Instant.parse("2026-11-01T10:00:00Z");

    private static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:3.9.1");

    static {
        KAFKA.start();
    }

    @DynamicPropertySource
    static void registerKafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);

        registry.add("app.kafka.enabled", () -> true);
        registry.add("app.kafka.client-events-topic", () -> TOPIC);
        registry.add("app.kafka.partitions", () -> 3);
        registry.add("app.kafka.replication-factor", () -> 1);

        registry.add("spring.kafka.consumer.group-id", () -> GROUP_ID);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.consumer.enable-auto-commit", () -> false);

        registry.add("spring.kafka.listener.auto-startup", () -> true);
        registry.add("spring.kafka.listener.concurrency", () -> 3);
        registry.add("spring.kafka.listener.ack-mode", () -> "manual_immediate");
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private KafkaListenerEndpointRegistry listenerRegistry;

    // проверка consumer, фоновая публикация Outbox здесь не нужна
    // listener, сервис обработки и репозитории остаются настоящими
    @MockitoBean
    private ClientEventOutboxScheduler outboxScheduler;

    @MockitoSpyBean
    private ClientEventProcessingService processingService;

    @Autowired
    private ClientEventOutboxPublisher outboxPublisher;

    @Autowired
    private ClientEventOutboxRepository outboxRepository;

    @BeforeEach
    void waitForListenerAssignment() {
        MessageListenerContainer container = listenerRegistry.getListenerContainer("clientChangedListener");

        assertNotNull(container, "Kafka listener должен быть зарегистрирован");

        ContainerTestUtils.waitForAssignment(container, 3);
    }

    @Test
    void shouldConsumeEventAndSaveProcessingResult() throws Exception {
        ClientChangedEvent event = createEvent();

        RecordMetadata metadata = sendEvent(event);

        awaitCommittedOffset(metadata);

        assertStoredCounts(event, 1L, 1L);

        String storedType = jdbcTemplate.queryForObject(
                """
                SELECT event_type
                FROM client_event_audit
                WHERE client_id = ?
                  AND event_id = ?
                """,
                String.class,
                event.clientId(),
                event.eventId()
        );

        assertEquals("CREATED", storedType);
    }

    @Test
    void shouldNotRepeatBusinessEffectForDuplicateEvent() throws Exception {
        ClientChangedEvent event = createEvent();

        RecordMetadata firstDelivery = sendEvent(event);

        awaitCommittedOffset(firstDelivery);
        assertStoredCounts(event, 1L, 1L);

        // отправка того же события с прежним eventId
        RecordMetadata secondDelivery = sendEvent(event);

        assertEquals(firstDelivery.partition(), secondDelivery.partition(), "Одинаковый clientId должен попасть в одну партицию");

        assertTrue(secondDelivery.offset() > firstDelivery.offset(), "Повторная отправка должна создать новую запись в Kafka");

        // тут я убедился, что consumer дошёл именно до второй доставки
        awaitCommittedOffset(secondDelivery);

        // в бд не должно появиться второго побочного эффекта
        assertStoredCounts(event, 1L, 1L);
    }

    @Test
    void shouldSendInvalidEventToDltWithoutBusinessEffect() throws Exception {
        ClientChangedEvent event = createEvent();

        ObjectNode payloadNode = objectMapper.valueToTree(event);
        payloadNode.remove("eventId");

        String invalidPayload = objectMapper.writeValueAsString(payloadNode);

        RecordMetadata sourceMetadata = sendPayload(event.clientId().toString(), invalidPayload);

        ConsumerRecord<String, String> deadLetter = awaitDltRecord(sourceMetadata, event.clientId().toString());

        assertEquals(event.clientId().toString(), deadLetter.key());
        assertEquals(invalidPayload, deadLetter.value());
        assertEquals(sourceMetadata.partition(), deadLetter.partition());

        awaitCommittedOffset(sourceMetadata);

        assertStoredCounts(event, 0L, 0L);
    }

    @Test
    void shouldProcessEventAfterTemporaryFailures() throws Exception {
        ClientChangedEvent event = createEvent();

        doThrow(new DataAccessResourceFailureException("Temporary processing failure: attempt 1"))
                .doThrow(new DataAccessResourceFailureException("Temporary processing failure: attempt 2"))
                .doCallRealMethod()
                .when(processingService)
                .process(event);

        RecordMetadata metadata = sendEvent(event);

        awaitCommittedOffset(metadata);

        verify(processingService, times(3)).process(event);

        assertStoredCounts(event, 1L, 1L);
    }

    @Test
    void shouldSendEventToDltAfterRetriesAndContinueConsuming() throws Exception {
        ClientChangedEvent failedEvent = createEvent();

        doThrow(new DataAccessResourceFailureException("Processing fails on every attempt"))
                .when(processingService)
                .process(failedEvent);

        String failedPayload = objectMapper.writeValueAsString(failedEvent);

        RecordMetadata failedMetadata = sendPayload(failedEvent.clientId().toString(), failedPayload);

        ConsumerRecord<String, String> deadLetter = awaitDltRecord(failedMetadata, failedEvent.clientId().toString());

        assertEquals(failedEvent.clientId().toString(), deadLetter.key());
        assertEquals(failedPayload, deadLetter.value());
        assertEquals(failedMetadata.partition(), deadLetter.partition());

        awaitCommittedOffset(failedMetadata);

        verify(processingService, times(3)).process(failedEvent);

        assertStoredCounts(failedEvent, 0L, 0L);

        // следующее событие того же клиента попадёт в ту же партицию
        ClientChangedEvent nextEvent = new ClientChangedEvent(UUID.randomUUID(), failedEvent.clientId(), ClientEventType.UPDATED, OCCURRED_AT.plusSeconds(1));

        RecordMetadata nextMetadata = sendEvent(nextEvent);

        assertEquals(failedMetadata.partition(), nextMetadata.partition());

        assertTrue(nextMetadata.offset() > failedMetadata.offset());

        awaitCommittedOffset(nextMetadata);

        verify(processingService).process(nextEvent);

        assertStoredCounts(nextEvent, 1L, 1L);
        assertStoredCounts(failedEvent, 0L, 0L);
    }

    @Test
    @DataSet(
            value = "datasets/client/kafka/outbox-retry.yml",
            cleanBefore = true,
            cleanAfter = true,
            disableConstraints = true,
            skipCleaningFor = {
                    "account_status",
                    "databasechangelog",
                    "databasechangeloglock"
            }
    )
    void shouldPublishOutboxEventAfterFailedAttempt() throws Exception {
        long outboxId = 900001L;

        ClientEventOutbox initial = outboxRepository
                .findById(outboxId)
                .orElseThrow();

        ClientChangedEvent event = initial.toEvent();
        String key = event.clientId().toString();
        String payload = objectMapper.writeValueAsString(event);

        CompletableFuture<SendResult<String, String>> failedSend = CompletableFuture.failedFuture(new KafkaException("Simulated Kafka send failure"));

        // первая попытка завершается ошибкой без отправки в брокер
        // вторая и последующие вызывают настоящий KafkaTemplate
        doReturn(failedSend)
                .doCallRealMethod()
                .when(kafkaTemplate)
                .send(TOPIC, key, payload);

        Instant beforeFailure = Instant.now();

        assertFalse(outboxPublisher.publishNext(), "Неудачная отправка должна вернуть false");

        // чтение состояния заново из БД после завершения транзакции publisher
        ClientEventOutbox failed = outboxRepository
                .findById(outboxId)
                .orElseThrow();

        assertNull(failed.getPublishedAt());
        assertEquals(1, failed.getAttempts());

        assertNotNull(failed.getLastError());
        assertTrue(failed.getLastError().contains("Simulated Kafka send failure"));

        assertTrue(failed.getNextAttemptAt().isAfter(beforeFailure), "После ошибки должна быть назначена следующая попытка");

        assertEquals(event, failed.toEvent());

        assertStoredCounts(event, 0L, 0L);

        // только в тесте делаем повторную попытку доступной немедленно
        int updated = jdbcTemplate.update(
                """
                UPDATE client_event_outbox
                SET next_attempt_at = CURRENT_TIMESTAMP - INTERVAL '1 second'
                WHERE id = ?
                  AND published_at IS NULL
                """,
                outboxId
        );

        assertEquals(1, updated);

        assertTrue(outboxPublisher.publishNext(), "Повторная отправка должна завершиться успешно");

        ClientEventOutbox published = outboxRepository
                .findById(outboxId)
                .orElseThrow();

        assertNotNull(published.getPublishedAt());
        assertEquals(2, published.getAttempts());
        assertNull(published.getLastError());

        assertEquals(event, published.toEvent(), "Повторная отправка не должна менять исходное событие");

        // обе попытки должны использовать одинаковый ключ и JSON
        verify(kafkaTemplate, times(2)).send(TOPIC, key, payload);

        // подтверждение брокера не означает завершение работы consumer
        // отдельно дожидаемся результата обработки в PostgreSQL
        await()
                .alias("Consumer должен обработать событие из Outbox")
                .atMost(WAIT_TIMEOUT)
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertStoredCounts(event, 1L, 1L));
    }

    private ClientChangedEvent createEvent() {
        return new ClientChangedEvent(UUID.randomUUID(), UUID.randomUUID(), ClientEventType.CREATED, OCCURRED_AT);
    }

    private RecordMetadata sendEvent(ClientChangedEvent event) throws Exception {
        return sendPayload(event.clientId().toString(), objectMapper.writeValueAsString(event));
    }

    private RecordMetadata sendPayload(String key, String payload) throws Exception {

        return kafkaTemplate
                .send(TOPIC, key, payload)
                .get(15, TimeUnit.SECONDS)
                .getRecordMetadata();
    }

    private void awaitCommittedOffset(RecordMetadata metadata) {
        TopicPartition partition = new TopicPartition(metadata.topic(), metadata.partition());

        Map<String, Object> adminProperties = Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());

        try (Admin admin = Admin.create(adminProperties)) {
            await()
                    .alias("Consumer должен подтвердить отправленное сообщение")
                    .atMost(WAIT_TIMEOUT)
                    .pollInterval(Duration.ofMillis(200))
                    .untilAsserted(() -> {
                        Map<TopicPartition, OffsetAndMetadata> offsets = admin.listConsumerGroupOffsets(GROUP_ID)
                                .partitionsToOffsetAndMetadata()
                                .get(5, TimeUnit.SECONDS);

                        OffsetAndMetadata committed = offsets.get(partition);

                        assertNotNull(committed, "Для партиции должен появиться committed offset");

                        // committed offset указывает на следующее сообщение
                        assertTrue(committed.offset() >= metadata.offset() + 1, "Consumer ещё не подтвердил проверяемое сообщение");
                    });
        }
    }

    private void assertStoredCounts(ClientChangedEvent event, long expectedProcessed, long expectedAudit) {
        Long processedCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM processed_client_event
                WHERE client_id = ?
                  AND event_id = ?
                """,
                Long.class,
                event.clientId(),
                event.eventId()
        );

        Long auditCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM client_event_audit
                WHERE client_id = ?
                  AND event_id = ?
                """,
                Long.class,
                event.clientId(),
                event.eventId()
        );

        assertEquals(Long.valueOf(expectedProcessed), processedCount);
        assertEquals(Long.valueOf(expectedAudit), auditCount);
    }

    private KafkaConsumer<String, String> createDltConsumer() {
        Map<String, Object> properties = Map.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),

                ConsumerConfig.GROUP_ID_CONFIG,
                "dlt-reader-" + UUID.randomUUID(),

                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                false,

                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest",

                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class,

                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        return new KafkaConsumer<>(properties);
    }

    private ConsumerRecord<String, String> awaitDltRecord(RecordMetadata sourceMetadata, String expectedKey) {
        TopicPartition partition = new TopicPartition(DLT_TOPIC, sourceMetadata.partition());

        try (KafkaConsumer<String, String> consumer = createDltConsumer()) {
            consumer.assign(List.of(partition));
            consumer.seekToBeginning(List.of(partition));

            long deadline = System.nanoTime() + WAIT_TIMEOUT.toNanos();

            while (System.nanoTime() < deadline) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {

                    if (expectedKey.equals(record.key())) {
                        return record;
                    }
                }
            }

            return fail("Не дождались сообщения в DLT с ключом " + expectedKey);
        }
    }
}
