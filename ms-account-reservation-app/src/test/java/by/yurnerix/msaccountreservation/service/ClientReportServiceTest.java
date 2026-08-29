package by.yurnerix.msaccountreservation.service;

import by.yurnerix.currencyclient.service.CurrencyService;
import by.yurnerix.msaccountreservation.config.AsyncProperties;
import by.yurnerix.msaccountreservation.exception.ClientReportTimeoutException;
import by.yurnerix.msaccountreservation.generated.dto.ClientDetailsResponseDto;
import by.yurnerix.msaccountreservation.generated.dto.ClientReportResponseDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class ClientReportServiceTest {

    private static final UUID CLIENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private ClientService clientService;

    @Mock
    private CurrencyService currencyService;

    private ExecutorService executor;
    private AsyncProperties asyncProperties;
    private ClientReportService clientReportService;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(3);

        asyncProperties = new AsyncProperties();
        asyncProperties.setTaskTimeout(Duration.ofSeconds(3));

        clientReportService = new ClientReportService(clientService, currencyService, executor, asyncProperties);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void getClientReportShouldCombineClientAndExchangeRates() {
        ClientDetailsResponseDto client = new ClientDetailsResponseDto();

        BigDecimal usdRubRate = new BigDecimal("91.25");
        BigDecimal eurRubRate = new BigDecimal("99.40");

        when(clientService.getClient(CLIENT_ID))
                .thenReturn(client);

        when(currencyService.getExchangeRate("USD", "RUB"))
                .thenReturn(usdRubRate);

        when(currencyService.getExchangeRate("EUR", "RUB"))
                .thenReturn(eurRubRate);

        ClientReportResponseDto result = clientReportService
                .getClientReport(CLIENT_ID)
                .join();

        assertSame(client, result.getClient());
        assertEquals(usdRubRate, result.getExchangeRates().get("USD/RUB"));
        assertEquals(eurRubRate, result.getExchangeRates().get("EUR/RUB"));

        verify(clientService).getClient(CLIENT_ID);
        verify(currencyService).getExchangeRate("USD", "RUB");
        verify(currencyService).getExchangeRate("EUR", "RUB");

    }

    @Test
    void getClientReportShouldRunTasksInParallel() throws Exception {
        ClientDetailsResponseDto client = new ClientDetailsResponseDto();

        BigDecimal usdRubRate = new BigDecimal("91.25");
        BigDecimal eurRubRate = new BigDecimal("99.40");

        CountDownLatch tasksStarted = new CountDownLatch(3);
        CountDownLatch releaseTasks = new CountDownLatch(1);

        when(clientService.getClient(CLIENT_ID))
                .thenAnswer(invocation -> awaitRelease(tasksStarted, releaseTasks, client));

        when(currencyService.getExchangeRate("USD", "RUB"))
                .thenAnswer(invocation -> awaitRelease(tasksStarted, releaseTasks, usdRubRate));

        when(currencyService.getExchangeRate("EUR", "RUB"))
                .thenAnswer(invocation -> awaitRelease(tasksStarted, releaseTasks, eurRubRate));

        CompletableFuture<ClientReportResponseDto> reportFuture = clientReportService.getClientReport(CLIENT_ID);

        boolean allTasksStarted = tasksStarted.await(1, TimeUnit.SECONDS);

        releaseTasks.countDown();

        assertTrue(allTasksStarted, "Все три операции должны выполняться параллельно");

        ClientReportResponseDto result = reportFuture.get(2, TimeUnit.SECONDS);

        assertSame(client, result.getClient());
        assertEquals(usdRubRate, result.getExchangeRates().get("USD/RUB"));
        assertEquals(eurRubRate, result.getExchangeRates().get("EUR/RUB"));
    }

    @Test
    void getClientReportShouldFailWithTimeoutException() {
        asyncProperties.setTaskTimeout(Duration.ofMillis(100));

        CountDownLatch blockedTask = new CountDownLatch(1);

        when(clientService.getClient(CLIENT_ID))
                .thenAnswer(invocation -> {
                    blockedTask.await(5, TimeUnit.SECONDS);
                    return new ClientDetailsResponseDto();
                });

        when(currencyService.getExchangeRate("USD", "RUB"))
                .thenReturn(new BigDecimal("91.25"));

        when(currencyService.getExchangeRate("EUR", "RUB"))
                .thenReturn(new BigDecimal("99.40"));

        CompletableFuture<ClientReportResponseDto> reportFuture = clientReportService.getClientReport(CLIENT_ID);

        CompletionException exception = assertThrows(CompletionException.class, reportFuture::join);

        assertInstanceOf(ClientReportTimeoutException.class, exception.getCause());
    }

    private <T> T awaitRelease(CountDownLatch tasksStarted, CountDownLatch releaseTasks, T result) throws InterruptedException {
        tasksStarted.countDown();

        boolean released = releaseTasks.await(2, TimeUnit.SECONDS);

        if (!released) {
            throw new IllegalStateException("Не удалось дождаться запуска параллельных задач");
        }

        return result;
    }

}
