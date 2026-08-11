package by.yurnerix.currencyclient.service;

import by.yurnerix.currencyclient.config.CurrencyClientProperties;
import by.yurnerix.currencyclient.exception.CurrencyClientException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DefaultCurrencyServiceTest {
    private static final String REQUEST_URL = "https://example.com/v6/test-api-key/pair/EUR/USD";

    private MockRestServiceServer mockServer;

    private CurrencyService currencyService;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();

        mockServer = MockRestServiceServer
                .bindTo(restTemplate)
                .build();

        CurrencyClientProperties properties = new CurrencyClientProperties();

        properties.setBaseUrl("https://example.com/v6");
        properties.setApiKey("test-api-key");

        currencyService = new DefaultCurrencyService(restTemplate, properties);
    }

    @AfterEach
    void verifyRequests() {
        mockServer.verify();
    }

    @Test
    void shouldReturnExchangeRate() {
        mockServer
                .expect(requestTo(REQUEST_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withSuccess(
                                """
                                        {
                                          "result": "success",
                                          "base_code": "EUR",
                                          "target_code": "USD",
                                          "conversion_rate": 1.1255
                                        }
                                        """,
                                MediaType.APPLICATION_JSON
                        )
                );

        BigDecimal result = currencyService.getExchangeRate("eur", "usd");

        assertThat(result).isEqualByComparingTo("1.1255");
    }

    @Test
    void shouldReturnOneForSameCurrencyWithoutHttpRequest() {
        BigDecimal result = currencyService.getExchangeRate("USD", "usd");

        assertThat(result).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void shouldThrowExceptionWhenApiReturnsError() {
        mockServer
                .expect(requestTo(REQUEST_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withSuccess(
                                """
                                        {
                                          "result": "error",
                                          "error-type": "unsupported-code"
                                        }
                                        """,
                                MediaType.APPLICATION_JSON
                        )
                );

        assertThatThrownBy(() ->
                currencyService.getExchangeRate("EUR", "USD"))
                .isInstanceOf(CurrencyClientException.class)
                .hasMessageContaining("unsupported-code");
    }

    @Test
    void shouldPropagateHttpError() {
        mockServer
                .expect(requestTo(REQUEST_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        assertThatThrownBy(() -> currencyService.getExchangeRate("EUR", "USD"))
                .isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void shouldRejectInvalidCurrencyCode() {
        assertThatThrownBy(() ->
                currencyService.getExchangeRate("EU", "USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly three Latin letters");
    }
}