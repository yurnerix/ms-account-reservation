package by.yurnerix.msaccountreservation.integration;

import by.yurnerix.currencyclient.client.CurrencyApiClient;
import by.yurnerix.currencyclient.dto.ExchangeRateResponse;
import by.yurnerix.currencyclient.service.CurrencyService;
import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class CurrencyServiceRedisCacheIntegrationTest extends AbstractMockedCurrencyIntegrationTest {

    private static final String API_KEY = "test-api-key";

    private static final String CACHE_NAME = "exchangeRates";

    private static final String REDIS_KEY = "exchangeRates::USD:EUR";

    private static final long EXPECTED_TTL_SECONDS = Duration.ofMinutes(30).toSeconds();

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    @Qualifier("currencyExchangeRateRequestsCounter")
    private Counter currencyExchangeRateRequestsCounter;

    @BeforeEach
    void clearCache() {
        Cache cache = cacheManager.getCache(CACHE_NAME);

        assertThat(cache).isNotNull();

        cache.clear();
    }

    @Test
    void shouldCacheExchangeRateInRedis() {
        ExchangeRateResponse response = createSuccessfulResponse();

        when(
                currencyApiClient.getExchangeRate(API_KEY, "USD", "EUR"))
                .thenReturn(response);

        double requestsBefore = currencyExchangeRateRequestsCounter.count();

        BigDecimal firstResult = currencyService.getExchangeRate("usd", "eur");

        BigDecimal secondResult = currencyService.getExchangeRate(" USD ", " EUR ");

        assertThat(firstResult)
                .isEqualByComparingTo("0.91");

        assertThat(secondResult)
                .isEqualByComparingTo("0.91");

        verify(currencyApiClient, times(1))
                .getExchangeRate(API_KEY, "USD", "EUR");

        assertThat(currencyExchangeRateRequestsCounter.count())
                .isEqualTo(requestsBefore + 1);

        assertThat(stringRedisTemplate.hasKey(REDIS_KEY))
                .isTrue();

        Long ttl = stringRedisTemplate.getExpire(REDIS_KEY, TimeUnit.SECONDS);

        assertThat(ttl)
                .isNotNull()
                .isBetween(1L, EXPECTED_TTL_SECONDS);
    }

    private ExchangeRateResponse createSuccessfulResponse() {
        ExchangeRateResponse response = new ExchangeRateResponse();

        response.setResult("success");
        response.setBaseCode("USD");
        response.setTargetCode("EUR");
        response.setConversionRate(new BigDecimal("0.91"));

        return response;
    }

}
