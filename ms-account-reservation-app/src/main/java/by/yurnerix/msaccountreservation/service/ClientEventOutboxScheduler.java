package by.yurnerix.msaccountreservation.service;

import by.yurnerix.msaccountreservation.config.ClientKafkaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ClientEventOutboxScheduler {

    private final ClientEventOutboxPublisher publisher;
    private final ClientKafkaProperties properties;

    @Scheduled(initialDelayString = "${app.kafka.outbox-poll-interval-ms:1000}", fixedDelayString = "${app.kafka.outbox-poll-interval-ms:1000}")
    public void publishPendingEvents() {
        for (int index = 0; index < properties.getOutboxBatchSize(); index++) {

            if (Thread.currentThread().isInterrupted()) {
                return;
            }

            try {
                boolean published = publisher.publishNext();

                if (!published) {
                    return;
                }
            } catch (RuntimeException exception) {
                log.error("Outbox publishing cycle failed", exception);

                return;
            }
        }
    }
}
