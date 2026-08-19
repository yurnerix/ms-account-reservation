package by.yurnerix.currencyclient.service;

import by.yurnerix.currencyclient.client.CurrencyApiClient;
import by.yurnerix.currencyclient.config.CurrencyClientProperties;
import by.yurnerix.currencyclient.dto.ExchangeRateResponse;
import by.yurnerix.currencyclient.exception.CurrencyClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public class DefaultCurrencyService implements CurrencyService {
    private static final String SUCCESS_RESULT = "success";

    private static final Pattern CURRENCY_CODE_PATTERN = Pattern.compile("^[A-Z]{3}$");

    private final CurrencyApiClient currencyApiClient;

    private final CurrencyClientProperties properties;

    private final RetryTemplate retryTemplate;

    @Override
    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        String from = normalizeCurrencyCode(fromCurrency);
        String to = normalizeCurrencyCode(toCurrency);

        if (from.equals(to)) {
            return BigDecimal.ONE;
        }

        validateApiKey();

        ExchangeRateResponse response = executeCurrencyRequest(from, to);

        return extractExchangeRate(response);
    }

    private ExchangeRateResponse executeCurrencyRequest(String from, String to) {
        return retryTemplate.execute(context -> {
            int attempt = context.getRetryCount() + 1;

            if (attempt > 1) {
                log.warn("Retrying currency API request: " + "from={}, to={}, attempt={}", from, to, attempt);
            }

            return currencyApiClient.getExchangeRate(properties.getApiKey(), from, to);
        });
    }

    private void validateApiKey() {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new CurrencyClientException("Currency client API key is not configured");
        }
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

        if (!CURRENCY_CODE_PATTERN.matcher(normalizedCurrency).matches()) {
            throw new IllegalArgumentException("Currency code must contain exactly " + "three Latin letters");
        }

        return normalizedCurrency;
    }
}