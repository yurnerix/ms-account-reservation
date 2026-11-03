package by.yurnerix.msaccountreservation.config;

import by.yurnerix.msaccountreservation.exception.InvalidClientEventException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.backoff.FixedBackOff;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(ClientKafkaProperties.class)
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ClientKafkaConfiguration {

    @Bean
    public NewTopic clientChangedTopic(ClientKafkaProperties properties) {
        return TopicBuilder
                .name(properties.getClientEventsTopic())
                .partitions(properties.getPartitions())
                .replicas(properties.getReplicationFactor())
                .build();
    }

    @Bean
    public NewTopic clientChangedDeadLetterTopic(ClientKafkaProperties properties) {
        return TopicBuilder
                .name(properties.getDeadLetterTopic())
                .partitions(properties.getPartitions())
                .replicas(properties.getReplicationFactor())
                .build();
    }

    @Bean
    public DefaultErrorHandler clientEventErrorHandler(KafkaTemplate<String, String> kafkaTemplate, ClientKafkaProperties properties) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate, (record, exception) -> new TopicPartition(properties.getDeadLetterTopic(), record.partition()));

        recoverer.setFailIfSendResultIsError(true); // ошибка отправки в DLT должна считаться ошибкой восстановления

        recoverer.setWaitForSendResultTimeout(Duration.ofSeconds(15)); // ожидание подтверждения отправки сообщения в DLT

        FixedBackOff backOff = new FixedBackOff(1000L, 2L); // первая попытка и 2 повторные, интервал между повторами 1 секунда

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        errorHandler.addNotRetryableExceptions(InvalidClientEventException.class); // некорректное сообщение, повторная обработка не исправит

        errorHandler.setCommitRecovered(true); // после успешной отправки в DLT подтверждаем исходное сообщение

        return errorHandler;
    }
}
