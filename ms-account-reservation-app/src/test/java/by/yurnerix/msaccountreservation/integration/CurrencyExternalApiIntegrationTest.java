package by.yurnerix.msaccountreservation.integration;

import by.yurnerix.msaccountreservation.service.ExchangeRateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(
        named = "CURRENCY_CLIENT_API_KEY",
        matches = ".+"
)
class CurrencyExternalApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Test
    void getExchangeRateShouldCallPublicApi() {
        BigDecimal rate = exchangeRateService.getExchangeRate("USD", "EUR");

        assertNotNull(rate);
        assertTrue(rate.compareTo(BigDecimal.ZERO) > 0);
    }

}
