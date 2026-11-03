package by.yurnerix.msaccountreservation.messaging;

import by.yurnerix.msaccountreservation.event.ClientChangedEvent;
import by.yurnerix.msaccountreservation.exception.InvalidClientEventException;
import by.yurnerix.msaccountreservation.service.ClientEventProcessingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ClientChangedEventListener {

    private final ObjectMapper objectMapper;
    private final ClientEventProcessingService processingService;

    @KafkaListener(id = "clientChangedListener", topics = "${app.kafka.client-events-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        ClientChangedEvent event = readEvent(record);

        boolean processed = processingService.process(event);

        // подтверждение offset только после успешного завершения транзакции в ClientEventProcessingService.
        acknowledgment.acknowledge();

        log.info(
                "Client event consumed: clientId={}, eventId={}, " +
                        "processed={}, partition={}, offset={}",
                event.clientId(),
                event.eventId(),
                processed,
                record.partition(),
                record.offset()
        );
    }

    private ClientChangedEvent readEvent(ConsumerRecord<String, String> record) {
        String payload = record.value();

        if (payload == null || payload.isBlank()) {
            throw new InvalidClientEventException(
                    "Client event payload must not be empty"
            );
        }

        ClientChangedEvent event;

        try {
            event = objectMapper.readValue(payload, ClientChangedEvent.class);
        } catch (JsonProcessingException exception) {
            throw new InvalidClientEventException("Cannot deserialize client event", exception);
        }

        if (event == null) {
            throw new InvalidClientEventException("Client event must not be null");
        }

        if (!event.clientId().toString().equals(record.key())) {
            throw new InvalidClientEventException("Kafka record key must match clientId");
        }

        return event;
    }
}
