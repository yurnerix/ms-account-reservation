package by.yurnerix.currencyclient.autoconfigure;


import by.yurnerix.currencyclient.config.CurrencyClientProperties;
import by.yurnerix.currencyclient.service.CurrencyService;
import by.yurnerix.currencyclient.service.DefaultCurrencyService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@AutoConfiguration
@AutoConfigureAfter(RestTemplateAutoConfiguration.class)
@ConditionalOnClass(RestTemplate.class)
@ConditionalOnProperty(
        prefix = "app.currency-client",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@EnableConfigurationProperties(CurrencyClientProperties.class)
public class CurrencyClientAutoConfiguration {

    @Bean("currencyClientRestTemplate")
    @ConditionalOnMissingBean(name = "currencyClientRestTemplate")
    public RestTemplate currencyClientRestTemplate(RestTemplateBuilder builder, CurrencyClientProperties properties) {
        return builder
                .connectTimeout(properties.getConnectTimeout())
                .readTimeout(properties.getReadTimeout())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(CurrencyService.class)
    public DefaultCurrencyService currencyService(@Qualifier("currencyClientRestTemplate") RestTemplate restTemplate, CurrencyClientProperties properties) {
        return new DefaultCurrencyService(restTemplate, properties);
    }



}
