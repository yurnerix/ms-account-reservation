package by.yurnerix.currencyclient.autoconfigure;

import by.yurnerix.currencyclient.exception.CurrencyClientException;
import by.yurnerix.currencyclient.health.CurrencyClientHealthIndicator;
import by.yurnerix.currencyclient.service.CurrencyService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;


import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

class CurrencyClientHealthAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CurrencyClientAutoConfiguration.CurrencyClientHealthConfiguration.class);

    @Test
    void shouldCreateUpHealthIndicator() {
        CurrencyService currencyService = mock(CurrencyService.class);

        when(currencyService.getExchangeRate("USD", "EUR"))
                .thenReturn(new BigDecimal("0.91"));

        contextRunner
                .withBean(CurrencyService.class, () -> currencyService)
                .withPropertyValues("app.currency-client.health.enabled=true")
                .run(context -> {
                    assertThat(context)
                            .hasSingleBean(CurrencyClientHealthIndicator.class);

                    CurrencyClientHealthIndicator indicator = context.getBean(CurrencyClientHealthIndicator.class);

                    Health health = indicator.health();

                    assertThat(health.getStatus())
                            .isEqualTo(Status.UP);

                    assertThat(health.getDetails())
                            .containsEntry("provider", "exchangerate-api");

                    assertThat(health.getDetails())
                            .containsEntry("exchangeRateAvailable", true);

                    verify(currencyService)
                            .getExchangeRate("USD", "EUR");
                });
    }

    @Test
    void shouldReturnDownWhenCurrencyApiFails() {
        CurrencyService currencyService = mock(CurrencyService.class);

        when(currencyService.getExchangeRate("USD", "EUR"))
                .thenThrow(new CurrencyClientException("External currency API is unavailable"));

        contextRunner
                .withBean(CurrencyService.class, () -> currencyService)
                .withPropertyValues("app.currency-client.health.enabled=true")
                .run(context -> {
                    CurrencyClientHealthIndicator indicator = context.getBean(CurrencyClientHealthIndicator.class);

                    Health health = indicator.health();

                    assertThat(health.getStatus())
                            .isEqualTo(Status.DOWN);

                    assertThat(health.getDetails())
                            .containsEntry("provider", "exchangerate-api");

                    assertThat(health.getDetails())
                            .containsKey("error");

                    verify(currencyService)
                            .getExchangeRate("USD", "EUR");
                });

    }

    @Test
    void shouldNotCreateHealthIndicatorWhenDisabled() {
        CurrencyService currencyService = mock(CurrencyService.class);

        contextRunner
                .withBean(CurrencyService.class, () -> currencyService)
                .withPropertyValues("app.currency-client.health.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(CurrencyClientHealthIndicator.class));
    }

}
