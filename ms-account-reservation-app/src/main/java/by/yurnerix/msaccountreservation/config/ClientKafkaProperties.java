package by.yurnerix.msaccountreservation.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.kafka")
public class ClientKafkaProperties {

    private boolean enabled = true;

    @NotBlank
    private String clientEventsTopic = "client-changed.v1"; // название топика

    @NotEmpty
    private List<@NotBlank String> clientConsumerTopics = new ArrayList<>(List.of("client-changed.v1"));

    @Min(1)
    private int partitions = 3; // количество партиций топика

    @Min(1)
    private short replicationFactor = 1; // количество копий каждой партиции

    @Min(1)
    private int outboxBatchSize = 20; // максимальное количество событий, которое отправитель может успешно обработать за один запуск цикла

    @Min(1)
    private long outboxPollIntervalMs = 1000; // между завершением одного цикла и началом следующего одна секунда

    @Min(1)
    private long outboxRetryDelayMs = 5000; // минимальная пауза перед повторной попыткой после ошибки - 5 секунд

    @Min(1)
    private long outboxSendTimeoutMs = 15000; // максимальное ожидание результата отправки

    @Min(1)
    private long dltSendTimeoutMs = 15000; // ожидание результата публикации в DLT

    @Min(0)
    private long consumerRetryIntervalMs = 1000; // интервал между повторными попытками consumer

    @Min(0)
    private long consumerRetryCount = 2; // количество повторов после первоначальной попытки

    public String getDeadLetterTopic() {
        return clientEventsTopic + ".DLT";
    }

    public List<String> getClientConsumerTopics() {
        return clientConsumerTopics.stream()
                .map(String::trim)
                .distinct()
                .toList();
    }
}
