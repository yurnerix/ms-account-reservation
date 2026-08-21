package by.yurnerix.currencyclient.autoconfigure;


import by.yurnerix.currencyclient.client.CurrencyApiClient;
import by.yurnerix.currencyclient.config.CurrencyClientProperties;
import by.yurnerix.currencyclient.health.CurrencyClientHealthIndicator;
import by.yurnerix.currencyclient.service.CurrencyService;
import by.yurnerix.currencyclient.service.DefaultCurrencyService;
import feign.Feign;
import feign.FeignException;
import feign.RetryableException;


import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;


@AutoConfiguration
@ConditionalOnClass({Feign.class, RetryTemplate.class})
@ConditionalOnProperty(
        prefix = "app.currency-client",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@EnableFeignClients(clients = CurrencyApiClient.class)
@EnableConfigurationProperties(CurrencyClientProperties.class)
public class CurrencyClientAutoConfiguration {

    private static final String EXCHANGE_RATE_REQUESTS_METRIC = "currency.exchange.rate.requests";

    @Bean("currencyClientRetryTemplate")
    @ConditionalOnMissingBean(name = "currencyClientRetryTemplate")
    public RetryTemplate currencyClientRetryTemplate(CurrencyClientProperties properties) {
        CurrencyClientProperties.RetryProperties retry = properties.getRetry();

        int maxAttempts = retry.isEnabled() ? retry.getMaxAttempts() : 1;

        return RetryTemplate.builder()
                .maxAttempts(maxAttempts)
                .exponentialBackoff(retry.getInitialInterval().toMillis(), retry.getMultiplier(), retry.getMaxInterval().toMillis())
                .retryOn(this::isRetryable)
                .build();
    }

    @Bean("currencyExchangeRateRequestsCounter")
    @ConditionalOnMissingBean(name = "currencyExchangeRateRequestsCounter")
    public Counter currencyExchangeRateRequestsCounter(MeterRegistry meterRegistry) {
        return Counter.builder(EXCHANGE_RATE_REQUESTS_METRIC)
                .description("Number of requests to the external currency API")
                .register(meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean(CurrencyService.class)
    public CurrencyService currencyService(CurrencyApiClient currencyApiClient, CurrencyClientProperties properties, @Qualifier("currencyClientRetryTemplate") RetryTemplate retryTemplate, @Qualifier("currencyExchangeRateRequestsCounter") Counter currencyExchangeRateRequestsCounter) {
        return new DefaultCurrencyService(currencyApiClient, properties, retryTemplate, currencyExchangeRateRequestsCounter);
    }

    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof RetryableException) {
            return true;
        }

        if (throwable instanceof FeignException exception) {
            int status = exception.status();

            return status == 429 || status >= 500;
        }

        return false;
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(HealthIndicator.class)
    @ConditionalOnProperty(prefix = "app.currency-client.health", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class CurrencyClientHealthConfiguration {

        @Bean("currencyClientHealthIndicator")
        @ConditionalOnMissingBean(name = "currencyClientHealthIndicator")
        CurrencyClientHealthIndicator currencyClientHealthIndicator(CurrencyService currencyService) {
            return new CurrencyClientHealthIndicator(currencyService);
        }
    }
}
