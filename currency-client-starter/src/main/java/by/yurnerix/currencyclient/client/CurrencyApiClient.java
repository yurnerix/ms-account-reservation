package by.yurnerix.currencyclient.client;

import by.yurnerix.currencyclient.config.CurrencyFeignConfiguration;
import by.yurnerix.currencyclient.dto.ExchangeRateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "currencyApiClient", url = "${app.currency-client.base-url}", configuration = CurrencyFeignConfiguration.class)
public interface CurrencyApiClient {

    @GetMapping("/{apiKey}/pair/{fromCurrency}/{toCurrency}")
    ExchangeRateResponse getExchangeRate(@PathVariable("apiKey") String apiKey,
                                         @PathVariable("fromCurrency") String fromCurrency,
                                         @PathVariable("toCurrency") String toCurrency);
}
