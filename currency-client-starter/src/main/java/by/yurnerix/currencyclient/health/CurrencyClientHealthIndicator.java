package by.yurnerix.currencyclient.health;


import by.yurnerix.currencyclient.service.CurrencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.math.BigDecimal;

@Slf4j
@RequiredArgsConstructor
public class CurrencyClientHealthIndicator implements HealthIndicator {

    private static final String PROVIDER = "exchangerate-api";

    private static final String HEALTH_FROM_CURRENCY = "USD";

    private static final String HEALTH_TO_CURRENCY = "EUR";

    private final CurrencyService currencyService;

    @Override
    public Health health() {
        try {
            BigDecimal exchangeRate = currencyService.getExchangeRate(HEALTH_FROM_CURRENCY, HEALTH_TO_CURRENCY);

            return Health.up()
                    .withDetail("provider", PROVIDER)
                    .withDetail("exchangeRateAvailable", exchangeRate != null)
                    .build();
        } catch (Exception exception) {
            log.warn("Currency API health check failed: " + "exceptionType={}", exception.getClass().getSimpleName());

            return Health.down()
                    .withDetail("provider", PROVIDER)
                    .withDetail("error", "Currency API is unavailable")
                    .build();
        }
    }
}
