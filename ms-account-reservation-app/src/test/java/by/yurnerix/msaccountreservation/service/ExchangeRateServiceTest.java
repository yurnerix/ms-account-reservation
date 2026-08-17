package by.yurnerix.msaccountreservation.service;

import by.yurnerix.currencyclient.exception.CurrencyClientException;
import by.yurnerix.currencyclient.service.CurrencyService;
import feign.FeignException;
import feign.RetryableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

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
        FeignException httpException = mock(FeignException.class);

        when(httpException.status())
                .thenReturn(429);

        when(currencyService.getExchangeRate("USD", "EUR"))
                .thenThrow(httpException);

        CurrencyClientException exception =
                assertThrows(CurrencyClientException.class, () -> exchangeRateService
                        .getExchangeRate("USD", "EUR"));

        assertEquals("Currency API request failed " + "with HTTP status 429", exception.getMessage());

        verify(currencyService)
                .getExchangeRate("USD", "EUR");
    }

    @Test
    void getExchangeRateShouldHandleConnectionError() {
        RetryableException connectionException = mock(RetryableException.class);

        when(currencyService.getExchangeRate("USD", "EUR"))
                .thenThrow(connectionException);

        CurrencyClientException exception = assertThrows(CurrencyClientException.class, () -> exchangeRateService.getExchangeRate("USD", "EUR"));

        assertEquals("Currency API is temporarily unavailable", exception.getMessage());

        verify(currencyService)
                .getExchangeRate("USD", "EUR");
    }



}
