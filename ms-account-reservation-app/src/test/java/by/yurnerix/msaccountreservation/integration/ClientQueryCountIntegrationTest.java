package by.yurnerix.msaccountreservation.integration;

import by.yurnerix.msaccountreservation.generated.dto.*;
import by.yurnerix.msaccountreservation.service.ClientService;
import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.junit5.api.DBRider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DBRider
@DataSet(value = "datasets/client/query-count.yml",
        cleanBefore = true,
        cleanAfter = true,
        disableConstraints = true,
        skipCleaningFor = {
                "account_status",
                "databasechangelog",
                "databasechangeloglock"
        })
@Transactional
@Isolated("Используется общий счётчик Hibernate Statistics")
@Execution(ExecutionMode.SAME_THREAD)
class ClientQueryCountIntegrationTest extends AbstractMockedCurrencyIntegrationTest {

    private static final String SEARCH_LAST_NAME = "UPG9QueryCount";
    private static final int SEARCH_CLIENTS_COUNT = 11;

    @Autowired
    private ClientService clientService;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @PersistenceContext
    private EntityManager entityManager;

    private Statistics statistics;

    @BeforeEach
    void setUpStatistics() {
        statistics = entityManagerFactory
                .unwrap(SessionFactory.class)
                .getStatistics();

        assertTrue(statistics.isStatisticsEnabled(), "В тестовом профиле должна быть включена статистика Hibernate");
    }

    @ParameterizedTest
    @CsvSource({
            "100, 0",
            "101, 1",
            "1, 5"
    })
    void getClientShouldUseOneStatement(long clientNumber, int accountsCount) {
        UUID clientId = new UUID(0L, clientNumber);

        startSqlCounting();

        ClientDetailsResponseDto response = clientService.getClient(clientId);

        long statementCount = statistics.getPrepareStatementCount();

        assertEquals(clientId, response.getId());
        assertEquals(ClientStatusDto.ACTIVE, response.getStatus());

        assertEquals(Boolean.valueOf(accountsCount > 0), response.getHasAccounts());

        assertNotNull(response.getAccounts());
        assertEquals(accountsCount, response.getAccounts().size());

        Set<AccountStatusDto> actualStatuses = response.getAccounts()
                .stream()
                .map(AccountResponseDto::getStatus)
                .collect(Collectors.toSet());

        assertEquals(expectedStatuses(accountsCount), actualStatuses);

        assertEquals(1L, statementCount, "Получение клиента должно выполнять один SQL-запрос");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10})
    void searchClientsShouldUseThreeStatements(int pageSize) {
        startSqlCounting();

        ClientPageResponseDto response = clientService.searchClients(0, pageSize, SEARCH_LAST_NAME, null);

        long statementCount = statistics.getPrepareStatementCount();

        assertNotNull(response.getContent());
        assertEquals(pageSize, response.getContent().size());

        assertNotNull(response.getPageable());
        assertEquals(0, response.getPageable().getPageNumber());
        assertEquals(pageSize, response.getPageable().getPageSize());

        int totalClients = SEARCH_CLIENTS_COUNT;

        assertEquals(Long.valueOf(totalClients), response.getPageable().getTotalElements());

        int expectedTotalPages = (totalClients + pageSize - 1) / pageSize;

        assertEquals(expectedTotalPages, response.getPageable().getTotalPages());

        for (int index = 0; index < pageSize; index++) {
            ClientSummaryDto actualClient = response.getContent().get(index);

            UUID expectedId = new UUID(0L, index + 1L);

            ClientStatusDto expectedStatus = index % 2 == 0 ? ClientStatusDto.ACTIVE : ClientStatusDto.BLOCKED;

            long expectedActiveAccountsCount = index % 2 == 0 ? 3L : 0L;

            assertEquals(expectedId, actualClient.getId());
            assertEquals(expectedStatus, actualClient.getStatus());

            assertEquals(Long.valueOf(expectedActiveAccountsCount), actualClient.getActiveAccountsCount());
        }

        assertEquals(3L, statementCount, "Поиск должен выполнять запрос страницы, COUNT клиентов " + "и один общий запрос количества активных счетов");
    }

    private void startSqlCounting() {
        entityManager.flush();
        entityManager.clear();
        statistics.clear();
    }

    private Set<AccountStatusDto> expectedStatuses(int accountsCount) {
        return switch (accountsCount) {
            case 0 -> Set.of();

            case 1 -> Set.of(AccountStatusDto.NEW);

            case 5 -> Set.of(
                    AccountStatusDto.NEW,
                    AccountStatusDto.IN_CREATION,
                    AccountStatusDto.CREATED,
                    AccountStatusDto.CANCELLED,
                    AccountStatusDto.CLOSED
            );

            default -> throw new IllegalArgumentException(
                    "Неизвестный тестовый сценарий: " + accountsCount
            );
        };
    }

}
