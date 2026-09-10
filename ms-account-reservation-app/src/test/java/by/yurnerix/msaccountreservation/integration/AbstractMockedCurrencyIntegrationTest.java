package by.yurnerix.msaccountreservation.integration;

import by.yurnerix.currencyclient.client.CurrencyApiClient;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@TestPropertySource(properties = {"app.currency-client.api-key=test-api-key"})
public abstract class AbstractMockedCurrencyIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    protected CurrencyApiClient currencyApiClient;
}
