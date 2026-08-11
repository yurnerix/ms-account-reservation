package by.yurnerix.currencyclient.autoconfigure;

import by.yurnerix.currencyclient.config.CurrencyClientProperties;
import by.yurnerix.currencyclient.service.CurrencyService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class CurrencyClientAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RestTemplateAutoConfiguration.class, CurrencyClientAutoConfiguration.class));

    @Test
    void shouldCreateCurrencyService() {
        contextRunner
                .withPropertyValues("app.currency-client.api-key=test-api-key")
                .run(context -> {
                    assertThat(context)
                            .hasSingleBean(CurrencyService.class);

                    assertThat(context)
                            .hasBean("currencyClientRestTemplate");

                    assertThat(context)
                            .hasSingleBean(CurrencyClientProperties.class);
                });
    }

    @Test
    void shouldBindCurrencyClientProperties() {
        contextRunner
                .withPropertyValues(
                        "app.currency-client.enabled=true",
                        "app.currency-client.base-url=https://example.com/v6",
                        "app.currency-client.api-key=test-api-key",
                        "app.currency-client.connect-timeout=3s",
                        "app.currency-client.read-timeout=7s"
                )
                .run(context -> {
                    CurrencyClientProperties properties = context.getBean(CurrencyClientProperties.class);

                    assertThat(properties.isEnabled()).isTrue();
                    assertThat(properties.getBaseUrl()).isEqualTo("https://example.com/v6");
                    assertThat(properties.getApiKey()).isEqualTo("test-api-key");
                    assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(3));
                    assertThat(properties.getReadTimeout()).isEqualTo(Duration.ofSeconds(7));
                });
    }

    @Test
    void shouldNotCreateCurrencyServiceWhenDisabled() {
        contextRunner
                .withPropertyValues("app.currency-client.enabled=false")
                .run(context ->
                        assertThat(context).doesNotHaveBean(CurrencyService.class));
    }

    @Test
    void shouldUseCustomCurrencyService()
    {
        contextRunner
                .withPropertyValues("app.currency-client.api-key=test-api-key")
                .withUserConfiguration(CustomCurrencyServiceConfiguration.class)
                .run(context -> {
                    assertThat(context)
                            .hasSingleBean(CurrencyService.class);

                    CurrencyService currencyService = context.getBean(CurrencyService.class);

                    assertThat(currencyService.getExchangeRate("USD", "EUR"))
                            .isEqualByComparingTo(BigDecimal.TEN);
                });
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CustomCurrencyServiceConfiguration {

        @Bean
        CurrencyService currencyService()
        {
            return ((fromCurrency, toCurrency) -> BigDecimal.TEN);
        }
    }
}
