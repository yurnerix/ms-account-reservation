package by.yurnerix.msaccountreservation.integration;

import by.yurnerix.currencyclient.service.CurrencyService;
import by.yurnerix.currencyclient.service.DefaultCurrencyService;
import by.yurnerix.msaccountreservation.service.ExchangeRateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CurrencyClientStarterIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Test
    void staterShouldRegisterCurrencyBeans() {
        assertNotNull(currencyService);
        assertNotNull(exchangeRateService);

        assertInstanceOf(DefaultCurrencyService.class, currencyService);
    }

}
