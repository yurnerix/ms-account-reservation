package by.yurnerix.msaccountreservation.integration;

import by.yurnerix.currencyclient.dto.ExchangeRateResponse;
import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.junit5.api.DBRider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DBRider
class ClientReportApiIntegrationTest extends AbstractMockedCurrencyIntegrationTest {

    private static final UUID CLIENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final String API_KEY = "test-api-key";
    private static final String CACHE_NAME = "exchangeRates";

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearExchangeRatesCache() {
        Cache cache = cacheManager.getCache(CACHE_NAME);

        assertNotNull(cache);

        cache.clear();
    }

    @Test
    @DataSet(
            value = "datasets/client/active-client.yml",
            cleanBefore = true,
            cleanAfter = true,
            disableConstraints = true,
            skipCleaningFor = {
                    "account_status",
                    "databasechangelog",
                    "databasechangeloglock"
            }
    )
    void getClientReportShouldReturnClientAndExchangeRates() throws Exception {
        Queue<String> currencyTaskThreads = new ConcurrentLinkedQueue<>();

        when(currencyApiClient.getExchangeRate(API_KEY, "USD", "RUB"))
                .thenAnswer(invocation -> {
                    currencyTaskThreads.add(Thread.currentThread().getName());

                    return createExchangeRateResponse("USD", "RUB", "91.25");
                });

        when(currencyApiClient.getExchangeRate(API_KEY, "EUR", "RUB"))
                .thenAnswer(invocation -> {
                    currencyTaskThreads.add(Thread.currentThread().getName());

                    return createExchangeRateResponse("EUR", "RUB", "99.40");
                });

        MvcResult mvcResult = mockMvc
                .perform(get("/api/v1/clients/{clientId}/report", CLIENT_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.client.id").value(CLIENT_ID.toString()))
                .andExpect(jsonPath("$.client.mdmId").value(8888888888L))
                .andExpect(jsonPath("$.client.firstName").value("Иван"))
                .andExpect(jsonPath("$.client.lastName").value("Петров"))
                .andExpect(jsonPath("$.client.middleName").value("Сергеевич"))
                .andExpect(jsonPath("$.client.citizenship").value("Россия"))
                .andExpect(jsonPath("$.client.status").value("ACTIVE"))
                .andExpect(jsonPath("$.client.hasAccounts").value(false))
                .andExpect(jsonPath("$.exchangeRates['USD/RUB']").value(91.25))
                .andExpect(jsonPath("$.exchangeRates['EUR/RUB']").value(99.40));

        verify(currencyApiClient)
                .getExchangeRate(API_KEY,"USD", "RUB");

        verify(currencyApiClient)
                .getExchangeRate(API_KEY,"EUR", "RUB");

        assertEquals(2, currencyTaskThreads.size());

        assertTrue(currencyTaskThreads
                        .stream()
                        .allMatch(threadName -> threadName.startsWith("client-report-")),
                "Валютные запросы должны выполняться " + "в пуле clientReportExecutor"
        );
    }

    private ExchangeRateResponse createExchangeRateResponse(String baseCurrency, String targetCurrency, String rate) {
        ExchangeRateResponse response = new ExchangeRateResponse();

        response.setResult("success");
        response.setBaseCode(baseCurrency);
        response.setTargetCode(targetCurrency);
        response.setConversionRate(new BigDecimal(rate));

        return response;
    }

}
