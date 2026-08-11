package by.yurnerix.msaccountreservation.service;

import by.yurnerix.currencyclient.exception.CurrencyClientException;
import by.yurnerix.currencyclient.service.CurrencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final CurrencyService currencyService;

    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {

        try {
            return currencyService.getExchangeRate(fromCurrency, toCurrency);
        } catch (RestClientResponseException exception) {
            log.error(
                    "Currency API returned HTTP status {} for currency pair {}/{}",
                    exception.getStatusCode().value(),
                    fromCurrency,
                    toCurrency
            );

            throw new CurrencyClientException("Currency API request failed with HTTP status " + exception.getStatusCode().value());

        } catch (RestClientException exception) {
            log.error(
                    "Currency API is unavailable for currency pair {}/{}",
                    fromCurrency,
                    toCurrency
            );

            throw new CurrencyClientException("Currency API is unavailable");
        }

    }

}
