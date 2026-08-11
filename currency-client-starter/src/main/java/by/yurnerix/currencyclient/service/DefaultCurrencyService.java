package by.yurnerix.currencyclient.service;

import by.yurnerix.currencyclient.config.CurrencyClientProperties;
import by.yurnerix.currencyclient.dto.ExchangeRateResponse;
import by.yurnerix.currencyclient.exception.CurrencyClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Locale;

@RequiredArgsConstructor
public class DefaultCurrencyService implements CurrencyService {
    private static final String SUCCESS_RESULT = "success";

    private final RestTemplate restTemplate;

    private final CurrencyClientProperties properties;

    @Override
    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        String from = normalizeCurrencyCode(fromCurrency);
        String to = normalizeCurrencyCode(toCurrency);

        if (from.equals(to)) {
            return BigDecimal.ONE;
        }

        ExchangeRateResponse response = restTemplate.getForObject(buildRequestUri(from, to), ExchangeRateResponse.class);

        return extractExchangeRate(response);
    }

    private URI buildRequestUri(String fromCurrency, String toCurrency) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new CurrencyClientException("Currency client API key is not configured");
        }

        return UriComponentsBuilder
                .fromUri(URI.create(properties.getBaseUrl()))
                .pathSegment(properties.getApiKey(), "pair", fromCurrency, toCurrency)
                .build()
                .encode()
                .toUri();
    }

    private BigDecimal extractExchangeRate(ExchangeRateResponse response) {
        if (response == null) {
            throw new CurrencyClientException("Currency API returned an empty response");
        }

        if (!SUCCESS_RESULT.equalsIgnoreCase(response.getResult())) {
            throw new CurrencyClientException("Currency API returned an error: " + response.getErrorType());
        }

        if (response.getConversionRate() == null) {
            throw new CurrencyClientException("Currency API response does not contain conversion_rate");
        }

        return response.getConversionRate();
    }

    private String normalizeCurrencyCode(String currency) {
        if (!StringUtils.hasText(currency)) {
            throw new IllegalArgumentException("Currency code must not be empty");
        }

        String normalizedCurrency = currency
                .trim()
                .toUpperCase(Locale.ROOT);

        if (!normalizedCurrency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Currency code must contain exactly three Latin letters");
        }

        return normalizedCurrency;
    }
}