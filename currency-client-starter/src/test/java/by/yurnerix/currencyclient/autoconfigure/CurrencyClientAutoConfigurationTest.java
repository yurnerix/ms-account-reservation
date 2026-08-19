package by.yurnerix.currencyclient.autoconfigure;

import by.yurnerix.currencyclient.client.CurrencyApiClient;
import by.yurnerix.currencyclient.config.CurrencyClientProperties;
import by.yurnerix.currencyclient.service.CurrencyService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.support.RetryTemplate;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class CurrencyClientAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FeignAutoConfiguration.class, CurrencyClientAutoConfiguration.class))
            .withPropertyValues("app.currency-client.base-url=https://example.com/v6");

    @Test
    void shouldCreateCurrencyService() {
        contextRunner
                .withPropertyValues("app.currency-client.api-key=" + "test-api-key")
                .run(context -> {
                    assertThat(context)
                            .hasSingleBean(CurrencyApiClient.class);

                    assertThat(context)
                            .hasSingleBean(CurrencyService.class);

                    assertThat(context)
                            .hasSingleBean(CurrencyClientProperties.class);

                    assertThat(context)
                            .hasSingleBean(RetryTemplate.class);

                    assertThat(context)
                            .hasBean("currencyClientRetryTemplate");
                });
    }

    @Test
    void shouldBindCurrencyClientProperties() {
        contextRunner
                .withPropertyValues(
                        "app.currency-client.enabled=true",
                        "app.currency-client.base-url=" + "https://example.com/v6",
                        "app.currency-client.api-key=" + "test-api-key",
                        "app.currency-client.connect-timeout=3s",
                        "app.currency-client.read-timeout=7s",
                        "app.currency-client.retry.enabled=true",
                        "app.currency-client.retry.max-attempts=4",
                        "app.currency-client.retry." + "initial-interval=300ms",
                        "app.currency-client.retry." + "multiplier=2.5",
                        "app.currency-client.retry." + "max-interval=3s",
                        "app.currency-client.health.enabled=false"
                )
                .run(context -> {
                    CurrencyClientProperties properties = context.getBean(CurrencyClientProperties.class);

                    assertThat(properties.isEnabled())
                            .isTrue();

                    assertThat(properties.getBaseUrl())
                            .isEqualTo("https://example.com/v6");

                    assertThat(properties.getApiKey())
                            .isEqualTo("test-api-key");

                    assertThat(properties.getConnectTimeout())
                            .isEqualTo(Duration.ofSeconds(3));

                    assertThat(properties.getReadTimeout())
                            .isEqualTo(Duration.ofSeconds(7));

                    assertThat(properties.getRetry().isEnabled())
                            .isTrue();

                    assertThat(properties.getRetry().getMaxAttempts())
                            .isEqualTo(4);

                    assertThat(properties.getRetry().getInitialInterval())
                            .isEqualTo(Duration.ofMillis(300));

                    assertThat(properties.getRetry().getMultiplier())
                            .isEqualTo(2.5);

                    assertThat(properties.getRetry().getMaxInterval())
                            .isEqualTo(Duration.ofSeconds(3));

                    assertThat(properties.getHealth().isEnabled())
                            .isFalse();
                });
    }

    @Test
    void shouldNotCreateCurrencyServiceWhenDisabled() {
        contextRunner
                .withPropertyValues("app.currency-client.enabled=false")
                .run(context -> {
                    assertThat(context)
                            .doesNotHaveBean(CurrencyApiClient.class);

                    assertThat(context)
                            .doesNotHaveBean(CurrencyService.class);

                    assertThat(context)
                            .doesNotHaveBean(RetryTemplate.class);
                });
    }

    @Test
    void shouldUseCustomCurrencyService()
    {
        contextRunner
                .withPropertyValues("app.currency-client.api-key=" + "test-api-key")
                .withUserConfiguration(CustomCurrencyServiceConfiguration.class)
                .run(context -> {
                    assertThat(context)
                            .hasSingleBean(CurrencyService.class);

                    CurrencyService currencyService = context.getBean(CurrencyService.class);

                    assertThat(currencyService
                            .getExchangeRate("USD", "EUR"))
                            .isEqualByComparingTo(
                            BigDecimal.TEN
                    );
                });
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CustomCurrencyServiceConfiguration {

        @Bean
        CurrencyService currencyService() {
            return ((fromCurrency, toCurrency) -> BigDecimal.TEN);
        }
    }
}
