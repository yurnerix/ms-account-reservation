package by.yurnerix.msaccountreservation.service;

import by.yurnerix.currencyclient.exception.CurrencyClientException;
import by.yurnerix.currencyclient.service.CurrencyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private CurrencyService currencyService;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    @Test
    void getExchangeRateShouldReturnRate() {
        BigDecimal expectedRate = new BigDecimal("0.9234");

        when(currencyService.getExchangeRate("USD", "EUR"))
                .thenReturn(expectedRate);

        BigDecimal actualRate = exchangeRateService.getExchangeRate("USD", "EUR");

        assertEquals(expectedRate, actualRate);

        verify(currencyService)
                .getExchangeRate("USD", "EUR");

    }

    @Test
    void getExchangeRateShouldHandleHttpError() {
        HttpClientErrorException httpException = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too Many Requests",
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8
                );

        when(currencyService.getExchangeRate("USD", "EUR"))
                .thenThrow(httpException);

        CurrencyClientException exception = assertThrows(CurrencyClientException.class, () -> exchangeRateService.getExchangeRate("USD", "EUR"));

        assertEquals("Currency API request failed with HTTP status 429", exception.getMessage());

        verify(currencyService)
                .getExchangeRate("USD", "EUR");

    }

    @Test
    void getExchangeRateShouldHandleConnectionError() {
        when(currencyService.getExchangeRate("USD", "EUR")).thenThrow(new ResourceAccessException("Connection timed out"));

        CurrencyClientException exception = assertThrows(CurrencyClientException.class, () -> exchangeRateService.getExchangeRate("USD", "EUR"));

        assertEquals("Currency API is unavailable", exception.getMessage());

        verify(currencyService)
                .getExchangeRate("USD", "EUR");
    }

}
