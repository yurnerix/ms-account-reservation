package by.yurnerix.msaccountreservation.service;

import by.yurnerix.msaccountreservation.config.ClientKafkaProperties;
import by.yurnerix.msaccountreservation.entity.ClientEventOutbox;
import by.yurnerix.msaccountreservation.repository.ClientEventOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ClientEventOutboxPublisher {

    private final ClientEventOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ClientKafkaProperties properties;

    @Transactional
    public boolean publishNext() {
        Optional<ClientEventOutbox> candidate = outboxRepository.findNextForPublishing();

        if (candidate.isEmpty()) {
            return false;
        }

        ClientEventOutbox outbox = candidate.get();

        SendResult<String, String> result;

        try {
            String payload = objectMapper.writeValueAsString(outbox.toEvent());

            result = kafkaTemplate.send(properties.getClientEventsTopic(), outbox.getClientId().toString(), payload)
                    .get(properties.getOutboxSendTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException("Outbox publication interrupted", exception);
        } catch (JsonProcessingException | ExecutionException | TimeoutException | RuntimeException exception) {
            recordFailure(outbox, exception);
            return false;
        }

        outbox.markPublished(Instant.now());
        outboxRepository.flush();

        log.info(
                "Kafka acknowledged client event: eventId={}, clientId={}, partition={}, offset={}",
                outbox.getEventId(),
                outbox.getClientId(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset()
        );

        return true;
    }

    private void recordFailure(ClientEventOutbox outbox, Exception exception) {
        Instant nextAttemptAt = Instant.now()
                .plusMillis(properties.getOutboxRetryDelayMs());

        outbox.markFailed(nextAttemptAt, exception.toString());

        outboxRepository.flush();

        log.warn(
                "Client event publication failed: eventId={}, clientId={}, nextAttemptAt={}",
                outbox.getEventId(),
                outbox.getClientId(),
                nextAttemptAt,
                exception
        );
    }
}
