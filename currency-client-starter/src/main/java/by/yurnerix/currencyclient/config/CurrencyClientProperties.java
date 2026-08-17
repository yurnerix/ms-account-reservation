package by.yurnerix.currencyclient.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.currency-client")
public class CurrencyClientProperties {

    private boolean enabled = true;

    private String baseUrl = "https://v6.exchangerate-api.com/v6";

    private String apiKey;

    private Duration connectTimeout = Duration.ofSeconds(2);

    private Duration readTimeout = Duration.ofSeconds(5);

    private RetryProperties retry = new RetryProperties();

    private HealthProperties health = new HealthProperties();

    @Getter
    @Setter
    public static class RetryProperties {

        private boolean enabled = true;

        private int maxAttempts = 3;

        private Duration initialInterval = Duration.ofMillis(200);

        private double multiplier = 2.0;

        private Duration maxInterval = Duration.ofSeconds(2);
    }

    @Getter
    @Setter
    public static class HealthProperties {

        private boolean enabled = true;
    }
}
