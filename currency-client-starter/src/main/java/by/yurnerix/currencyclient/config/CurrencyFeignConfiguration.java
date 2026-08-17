package by.yurnerix.currencyclient.config;

import feign.Request;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

public class CurrencyFeignConfiguration {

    @Bean
    public Request.Options currencyClientRequestOptions(CurrencyClientProperties properties) {
        return new Request.Options(properties.getConnectTimeout().toMillis(), TimeUnit.MILLISECONDS, properties.getReadTimeout().toMillis(), TimeUnit.MILLISECONDS, true);
    }

}
