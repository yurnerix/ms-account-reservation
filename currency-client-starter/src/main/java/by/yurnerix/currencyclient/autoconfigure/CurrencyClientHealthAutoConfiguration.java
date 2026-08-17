package by.yurnerix.currencyclient.autoconfigure;

import by.yurnerix.currencyclient.health.CurrencyClientHealthIndicator;
import by.yurnerix.currencyclient.service.CurrencyService;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = CurrencyClientAutoConfiguration.class)
@ConditionalOnClass(HealthIndicator.class)
@ConditionalOnBean(CurrencyService.class)
@ConditionalOnProperty(
        prefix = "app.currency-client.health",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class CurrencyClientHealthAutoConfiguration {

    @Bean("currencyClientHealthIndicator")
    @ConditionalOnMissingBean(name = "currencyClientHealthIndicator")
    public CurrencyClientHealthIndicator currencyClientHealthIndicator(CurrencyService currencyService) {
        return new CurrencyClientHealthIndicator(currencyService);
    }
}
