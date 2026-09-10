package by.yurnerix.msaccountreservation.service;

import by.yurnerix.currencyclient.service.CurrencyService;
import by.yurnerix.msaccountreservation.config.AsyncProperties;
import by.yurnerix.msaccountreservation.config.ClientReportAsyncConfiguration;
import by.yurnerix.msaccountreservation.exception.ClientReportTimeoutException;
import by.yurnerix.msaccountreservation.generated.dto.ClientDetailsResponseDto;
import by.yurnerix.msaccountreservation.generated.dto.ClientReportResponseDto;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Supplier;


@Slf4j
@Service
@RequiredArgsConstructor
public class ClientReportService {

    private static final String USD = "USD";
    private static final String EUR = "EUR";
    private static final String RUB = "RUB";
    private static final String USD_RUB_PAIR = "USD/RUB";
    private static final String EUR_RUB_PAIR = "EUR/RUB";

    private final ClientService clientService;
    private final CurrencyService currencyService;

    @Qualifier(ClientReportAsyncConfiguration.CLIENT_REPORT_EXECUTOR)
    private final Executor clientReportExecutor;

    private final AsyncProperties asyncProperties;

    public CompletableFuture<ClientReportResponseDto> getClientReport(UUID clientId) {
        log.info(
                "Client report generation started: clientId={}",
                clientId
        );

        return ClientReportTasks.builder()
                .client(submitTask(clientId, "client", () -> clientService.getClient(clientId)))
                .usdRub(submitTask(clientId, USD_RUB_PAIR, () -> currencyService.getExchangeRate(USD, RUB)))
                .eurRub(submitTask(clientId, EUR_RUB_PAIR, () -> currencyService.getExchangeRate(EUR, RUB)))
                .build()
                .combine()
                .orTimeout(asyncProperties.getTaskTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .handle((report, throwable) -> handleResult(clientId, report, throwable));
    }

    private <T> CompletableFuture<T> submitTask(UUID clientId, String taskName, Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Client report task started: " + "clientId={}, task={}, thread={}", clientId, taskName, Thread.currentThread().getName());

            try {
                return supplier.get();
            } finally {
                log.debug("Client report task finished: " + "clientId={}, task={}, thread={}", clientId, taskName, Thread.currentThread().getName());
            }
            },
                clientReportExecutor
        );
    }

    private ClientReportResponseDto handleResult(UUID clientId, ClientReportResponseDto report, Throwable throwable) {
        if (throwable == null) {
            log.info(
                    "Client report generated successfully: clientId={}",
                    clientId
            );

            return report;
        }

        Throwable cause = unwrapException(throwable);

        if(cause instanceof TimeoutException) {
            log.warn(
                    "Client report generation timed out: " + "clientId={}, timeout={}",
                    clientId,
                    asyncProperties.getTaskTimeout()
            );

            throw new ClientReportTimeoutException(clientId, asyncProperties.getTaskTimeout());
        }

        if (cause instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }

        throw new CompletionException(cause);
    }

    private Throwable unwrapException(Throwable throwable) {
        Throwable current = throwable;

        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }

        return current;
    }

    @Builder
    private static final class ClientReportTasks {

        private final CompletableFuture<ClientDetailsResponseDto> client;
        private final CompletableFuture<BigDecimal> usdRub;
        private final CompletableFuture<BigDecimal> eurRub;

        private CompletableFuture<ClientReportResponseDto> combine() {
            return client.thenCombine(usdRub.thenCombine(eurRub, ClientReportService::createExchangeRates), ClientReportService::createReport);
        }
    }

    private static Map<String, BigDecimal> createExchangeRates(BigDecimal usdRub, BigDecimal eurRub) {
        Map<String, BigDecimal> exchangeRates = new LinkedHashMap<>();

        exchangeRates.put(USD_RUB_PAIR, usdRub);
        exchangeRates.put(EUR_RUB_PAIR, eurRub);

        return exchangeRates;
    }

    private static ClientReportResponseDto createReport(ClientDetailsResponseDto client, Map<String, BigDecimal> exchangeRates) {
        return new ClientReportResponseDto()
                .client(client)
                .exchangeRates(exchangeRates);
    }

}
