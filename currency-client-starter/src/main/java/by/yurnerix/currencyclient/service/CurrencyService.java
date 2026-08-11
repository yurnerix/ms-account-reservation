package by.yurnerix.currencyclient.service;

import java.math.BigDecimal;

public interface CurrencyService {
    BigDecimal getExchangeRate(String fromCurrency, String toCurrency);
}
