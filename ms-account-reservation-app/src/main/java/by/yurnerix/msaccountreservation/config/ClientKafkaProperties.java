package by.yurnerix.msaccountreservation.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.kafka")
public class ClientKafkaProperties {

    private boolean enabled = true;

    @NotBlank
    private String clientEventsTopic = "client-changed.v1"; // название топика

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

    public String getDeadLetterTopic() {
        return clientEventsTopic + ".DLT";
    }
}
