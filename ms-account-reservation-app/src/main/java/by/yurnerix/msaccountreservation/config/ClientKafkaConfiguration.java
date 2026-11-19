package by.yurnerix.msaccountreservation.config;

import by.yurnerix.msaccountreservation.exception.InvalidClientEventException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ClientKafkaProperties.class)
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ClientKafkaConfiguration {

    @Bean
    public List<String> clientEventConsumerTopics(ClientKafkaProperties properties) {
        return properties.getClientConsumerTopics();
    }

    @Bean
    public KafkaAdmin.NewTopics clientEventTopics(ClientKafkaProperties properties) {
        Set<String> sourceTopics = new LinkedHashSet<>(properties.getClientConsumerTopics());

        // outbox может публиковать в топик на который этот consumer не подписан
        sourceTopics.add(properties.getClientEventsTopic());

        NewTopic[] topics = sourceTopics.stream()
                .flatMap(topic -> Stream.of(topic, topic + ".DLT"))
                .distinct()
                .map(topic -> TopicBuilder
                        .name(topic)
                        .partitions(properties.getPartitions())
                        .replicas(properties.getReplicationFactor())
                        .build())
                .toArray(NewTopic[]::new);

        return new KafkaAdmin.NewTopics(topics);
    }

    @Bean
    public DefaultErrorHandler clientEventErrorHandler(KafkaTemplate<String, String> kafkaTemplate, ClientKafkaProperties properties) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate, (record, exception) -> new TopicPartition(record.topic() + ".DLT", record.partition()));

        recoverer.setFailIfSendResultIsError(true);

        recoverer.setWaitForSendResultTimeout(Duration.ofMillis(properties.getDltSendTimeoutMs()));

        FixedBackOff backOff = new FixedBackOff(properties.getConsumerRetryIntervalMs(), properties.getConsumerRetryCount());

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);


        errorHandler.addNotRetryableExceptions(InvalidClientEventException.class);

        errorHandler.setCommitRecovered(true);

        return errorHandler;
    }
}
