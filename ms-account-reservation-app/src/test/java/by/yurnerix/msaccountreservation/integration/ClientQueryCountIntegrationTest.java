package by.yurnerix.msaccountreservation.integration;

import by.yurnerix.msaccountreservation.entity.Account;
import by.yurnerix.msaccountreservation.entity.AccountStatus;
import by.yurnerix.msaccountreservation.entity.AccountStatusName;
import by.yurnerix.msaccountreservation.entity.Client;
import by.yurnerix.msaccountreservation.entity.ClientStatus;
import by.yurnerix.msaccountreservation.generated.dto.AccountResponseDto;
import by.yurnerix.msaccountreservation.generated.dto.AccountStatusDto;
import by.yurnerix.msaccountreservation.generated.dto.ClientDetailsResponseDto;
import by.yurnerix.msaccountreservation.generated.dto.ClientPageResponseDto;
import by.yurnerix.msaccountreservation.generated.dto.ClientStatusDto;
import by.yurnerix.msaccountreservation.generated.dto.ClientSummaryDto;
import by.yurnerix.msaccountreservation.repository.AccountRepository;
import by.yurnerix.msaccountreservation.repository.AccountStatusRepository;
import by.yurnerix.msaccountreservation.repository.ClientRepository;
import by.yurnerix.msaccountreservation.service.ClientService;
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
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
@Isolated("Используется общий счётчик Hibernate Statistics")
@Execution(ExecutionMode.SAME_THREAD)
class ClientQueryCountIntegrationTest extends AbstractMockedCurrencyIntegrationTest {

    private static final String TEST_LAST_NAME = "UPG9QueryCount";
    private static final long MDM_ID_BASE = 9_900_000_000L;

    @Autowired
    private ClientService clientService;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountStatusRepository accountStatusRepository;

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
    @ValueSource(ints = {0, 1, 5})
    void getClientShouldUseOneStatement(int accountsCount) {
        Client client = saveClient(MDM_ID_BASE, "Details");

        AccountStatusName[] statuses = AccountStatusName.values();

        Set<AccountStatusDto> expectedStatuses = EnumSet.noneOf(AccountStatusDto.class);

        for (int index = 0; index < accountsCount; index++) {
            AccountStatusName status = statuses[index % statuses.length];

            saveAccount(client, status);

            expectedStatuses.add(AccountStatusDto.fromValue(status.name()));
        }

        UUID clientId = client.getId();

        startSqlCounting();

        ClientDetailsResponseDto response = clientService.getClient(clientId);

        long statementCount = statistics.getPrepareStatementCount();

        assertEquals(clientId, response.getId());
        assertEquals(ClientStatusDto.ACTIVE, response.getStatus());
        assertEquals(Boolean.valueOf(accountsCount > 0), response.getHasAccounts());

        assertNotNull(response.getAccounts());
        assertEquals(accountsCount, response.getAccounts().size());

        Set<AccountStatusDto> actualStatuses = Set.copyOf(
                response.getAccounts()
                        .stream()
                        .map(AccountResponseDto::getStatus)
                        .toList()
        );

        assertEquals(expectedStatuses, actualStatuses);

        assertEquals(1L, statementCount, "Получение клиента должно выполнять один SQL-запрос");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10})
    void searchClientsShouldUseTwoStatements(int pageSize) {
        List<Client> clients = new ArrayList<>();

        for (int index = 0; index < pageSize + 1; index++) {
            Client client = saveClient(MDM_ID_BASE + index, String.format(Locale.ROOT, "Client%02d", index));

            if (index % 2 == 0) {
                for (AccountStatusName status : AccountStatusName.values()) {
                    saveAccount(client, status);
                }
            } else {
                client.setStatus(ClientStatus.BLOCKED);
            }

            clients.add(client);
        }


        Client deletedClient = saveClient(MDM_ID_BASE + 1_000, "Deleted");
        deletedClient.setStatus(ClientStatus.DELETED);


        Client otherClient = saveClient(MDM_ID_BASE + 2_000, "Other");
        otherClient.setLastName("OtherQueryGroup");

        startSqlCounting();

        ClientPageResponseDto response = clientService.searchClients(0, pageSize, TEST_LAST_NAME, null);

        long statementCount = statistics.getPrepareStatementCount();

        assertNotNull(response.getContent());
        assertEquals(pageSize, response.getContent().size());

        assertNotNull(response.getPageable());
        assertEquals(0, response.getPageable().getPageNumber());
        assertEquals(pageSize, response.getPageable().getPageSize());
        assertEquals(Long.valueOf(pageSize + 1L), response.getPageable().getTotalElements());
        assertEquals(2, response.getPageable().getTotalPages());

        for (int index = 0; index < pageSize; index++) {
            Client expectedClient = clients.get(index);
            ClientSummaryDto actualClient = response.getContent().get(index);

            long expectedActiveAccountsCount = index % 2 == 0 ? 3L : 0L;

            assertEquals(expectedClient.getId(), actualClient.getId());
            assertEquals(ClientStatusDto.fromValue(expectedClient.getStatus().name()), actualClient.getStatus());
            assertEquals(Long.valueOf(expectedActiveAccountsCount), actualClient.getActiveAccountsCount());
        }

        assertEquals(2L, statementCount, "Поиск должен выполнять основной запрос и один COUNT");
    }

    private void startSqlCounting() {
        entityManager.flush();

        entityManager.clear();

        statistics.clear();
    }

    private Client saveClient(long mdmId, String firstName) {
        Client client = Client.builder()
                .mdmId(mdmId)
                .firstName(firstName)
                .lastName(TEST_LAST_NAME)
                .middleName("Тестович")
                .citizenship("Россия")
                .clientType("INDIVIDUAL")
                .documentNumber(Long.toString(mdmId))
                .documentSeries("1234")
                .documentType("PASSPORT")
                .build();

        return clientRepository.save(client);
    }

    private void saveAccount(Client client, AccountStatusName statusName) {
        AccountStatus status = accountStatusRepository
                .findByName(statusName)
                .orElseThrow();

        Account account = new Account(status, client, "CURRENT", "RUB");

        accountRepository.save(account);
        client.getAccounts().add(account);
    }

}
