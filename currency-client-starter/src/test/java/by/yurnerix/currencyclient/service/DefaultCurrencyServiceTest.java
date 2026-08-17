package by.yurnerix.currencyclient.service;

import by.yurnerix.currencyclient.autoconfigure.CurrencyClientAutoConfiguration;
import by.yurnerix.currencyclient.client.CurrencyApiClient;
import by.yurnerix.currencyclient.config.CurrencyClientProperties;
import by.yurnerix.currencyclient.dto.ExchangeRateResponse;
import by.yurnerix.currencyclient.exception.CurrencyClientException;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.support.RetryTemplate;


import java.math.BigDecimal;
import java.time.Duration;

import static by.yurnerix.currencyclient.util.MockUtils.readJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultCurrencyServiceTest {

    private static final String API_KEY = "test-api-key";

    @Mock
    private CurrencyApiClient currencyApiClient;

    private CurrencyClientProperties properties;

    private CurrencyService currencyService;


    @BeforeEach
    void setUp() {
        properties = new CurrencyClientProperties();

        properties.setBaseUrl("https://example.com/v6");
        properties.setApiKey(API_KEY);

        properties.getRetry().setMaxAttempts(1);
        properties.getRetry().setInitialInterval(Duration.ofMillis(1));
        properties.getRetry().setMaxInterval(Duration.ofMillis(2));

        currencyService = createCurrencyService();
    }



    @Test
    void shouldReturnExchangeRate() {
        ExchangeRateResponse response = readJson("responses/exchange-rate-success.json", ExchangeRateResponse.class);

        when(currencyApiClient.getExchangeRate(API_KEY, "EUR", "USD")).thenReturn(response);

        BigDecimal result = currencyService.getExchangeRate("eur", "usd");

        assertThat(result).isEqualByComparingTo("1.1255");

        verify(currencyApiClient).getExchangeRate(API_KEY, "EUR", "USD");
    }

    @Test
    void shouldReturnOneForSameCurrencyWithoutHttpRequest() {
        BigDecimal result = currencyService.getExchangeRate("USD", "usd");

        assertThat(result).isEqualByComparingTo(BigDecimal.ONE);

        verifyNoInteractions(currencyApiClient);

    }

    @Test
    void shouldThrowExceptionWhenApiReturnsError() {
        ExchangeRateResponse response = readJson("responses/exchange-rate-error.json", ExchangeRateResponse.class);

        when(currencyApiClient.getExchangeRate(API_KEY, "EUR", "USD")).thenReturn(response);

        assertThatThrownBy(() -> currencyService.getExchangeRate("EUR", "USD"))
                .isInstanceOf(CurrencyClientException.class)
                .hasMessageContaining("unsupported-code");
    }

    @Test
    void shouldPropagateHttpError() {
        FeignException exception = createFeignException(500);

        when(currencyApiClient.getExchangeRate(API_KEY, "EUR", "USD")).thenThrow(exception);

        assertThatThrownBy(() -> currencyService.getExchangeRate("EUR", "USD")).isSameAs(exception);
    }

    @Test
    void shouldRetryTemporaryHttpError() {
        properties.getRetry().setMaxAttempts(3);

        CurrencyService serviceWithRetry = createCurrencyService();

        FeignException exception = createFeignException(500);

        when(currencyApiClient.getExchangeRate(API_KEY, "EUR", "USD")).thenThrow(exception);

        assertThatThrownBy(() -> serviceWithRetry.getExchangeRate("EUR", "USD")).isSameAs(exception);

        verify(currencyApiClient, times(3)).getExchangeRate(API_KEY, "EUR", "USD");
    }

    @Test
    void shouldNotRetryClientHttpError() {
        properties.getRetry().setMaxAttempts(3);

        CurrencyService serviceWithRetry = createCurrencyService();

        FeignException exception = createFeignException(401);

        when(currencyApiClient.getExchangeRate(API_KEY, "EUR", "USD")).thenThrow(exception);

        assertThatThrownBy(() -> serviceWithRetry.getExchangeRate("EUR", "USD")).isSameAs(exception);

        verify(currencyApiClient, times(1)).getExchangeRate(API_KEY, "EUR", "USD");
    }

    @Test
    void shouldRejectInvalidCurrencyCode() {
        assertThatThrownBy(() -> currencyService.getExchangeRate("EU", "USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly three Latin letters");

        verifyNoInteractions(currencyApiClient);
    }

    @Test
    void shouldRejectRequestWhenApiKeyIsMissing() {
        properties.setApiKey(null);

        assertThatThrownBy(() -> currencyService.getExchangeRate("EUR", "USD"))
                .isInstanceOf(CurrencyClientException.class)
                .hasMessageContaining("API key is not configured");

        verifyNoInteractions(currencyApiClient);
    }

    private CurrencyService createCurrencyService() {
        CurrencyClientAutoConfiguration autoConfiguration = new CurrencyClientAutoConfiguration();

        RetryTemplate retryTemplate = autoConfiguration.currencyClientRetryTemplate(properties);

        return new DefaultCurrencyService(currencyApiClient, properties, retryTemplate);
    }

    private FeignException createFeignException(int status) {
        FeignException exception = mock(FeignException.class);

        when(exception.status()).thenReturn(status);

        return exception;
    }
}
