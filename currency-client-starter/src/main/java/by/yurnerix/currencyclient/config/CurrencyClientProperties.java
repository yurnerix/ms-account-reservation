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
}