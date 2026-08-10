package by.yurnerix.msaccountreservation.integration;

import by.yurnerix.msaccountreservation.entity.Account;
import by.yurnerix.msaccountreservation.entity.AccountStatus;
import by.yurnerix.msaccountreservation.entity.Client;
import by.yurnerix.msaccountreservation.entity.ClientStatus;
import by.yurnerix.msaccountreservation.entity.AccountStatusName;
import by.yurnerix.msaccountreservation.generated.dto.ClientResponseDto;
import by.yurnerix.msaccountreservation.generated.dto.CreateClientRequestDto;
import by.yurnerix.msaccountreservation.generated.dto.UpdateClientRequestDto;
import by.yurnerix.msaccountreservation.repository.AccountRepository;
import by.yurnerix.msaccountreservation.repository.AccountStatusRepository;
import by.yurnerix.msaccountreservation.repository.ClientRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.junit5.api.DBRider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DBRider
@Transactional
class ClientApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AccountStatusRepository accountStatusRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void contextShouldStartWithMigratedDatabase() {
        assertAll(
                () -> assertEquals(5, accountStatusRepository.count()),
                () -> assertTrue(accountStatusRepository.findByName(AccountStatusName.NEW).isPresent()),
                () -> assertTrue(accountStatusRepository.findByName(AccountStatusName.IN_CREATION).isPresent()),
                () -> assertTrue(accountStatusRepository.findByName(AccountStatusName.CREATED).isPresent()),
                () -> assertTrue(accountStatusRepository.findByName(AccountStatusName.CANCELLED).isPresent()),
                () -> assertTrue(accountStatusRepository.findByName(AccountStatusName.CLOSED).isPresent()));
    }

    @Test
    void createClientShouldPersistClientAndReturn201() throws Exception {
        CreateClientRequestDto request = new CreateClientRequestDto();

        request.setMdmId(9876543210L);
        request.setFirstName("Пётр");
        request.setLastName("Иванов");
        request.setMiddleName("Андреевич");
        request.setCitizenship("Россия");
        request.setClientType("INDIVIDUAL");
        request.setDocumentNumber("654321");
        request.setDocumentSeries("4321");
        request.setDocumentType("PASSPORT");

        String responseBody = mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))

                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.mdmId").value(9876543210L))
                .andExpect(jsonPath("$.firstName").value("Пётр"))
                .andExpect(jsonPath("$.lastName").value("Иванов"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ClientResponseDto response = objectMapper.readValue(responseBody, ClientResponseDto.class);

        assertNotNull(response.getId());

        Client savedClient = clientRepository
                .findById(response.getId())
                .orElseThrow();

        assertAll(
                () -> assertEquals(9876543210L, savedClient.getMdmId()),
                () -> assertEquals("Пётр", savedClient.getFirstName()),
                () -> assertEquals("Иванов", savedClient.getLastName()),
                () -> assertEquals("Андреевич", savedClient.getMiddleName()),
                () -> assertEquals("Россия", savedClient.getCitizenship()),
                () -> assertEquals("INDIVIDUAL", savedClient.getClientType()),
                () -> assertEquals("654321", savedClient.getDocumentNumber()),
                () -> assertEquals("4321", savedClient.getDocumentSeries()),
                () -> assertEquals("PASSPORT", savedClient.getDocumentType()),
                () -> assertEquals(ClientStatus.ACTIVE, savedClient.getStatus()),
                () -> assertNotNull(savedClient.getCreatedAt()),
                () -> assertNotNull(savedClient.getUpdatedAt())
        );
    }

    private CreateClientRequestDto createValidRequest(Long mdmId) {
        CreateClientRequestDto request = new CreateClientRequestDto();

        request.setMdmId(mdmId);
        request.setFirstName("Пётр");
        request.setLastName("Иванов");
        request.setMiddleName("Андреевич");
        request.setCitizenship("Россия");
        request.setClientType("INDIVIDUAL");
        request.setDocumentNumber("654321");
        request.setDocumentSeries("4321");
        request.setDocumentType("PASSPORT");

        return request;
    }

    @Test
    void createClientShouldReturn409WhenMdmIdAlreadyExists() throws Exception {
        Long mdmId = 7777777777L;

        CreateClientRequestDto request = createValidRequest(mdmId);

        String requestBody = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(requestBody)).andExpect(status()
                .isCreated());

        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(requestBody))

                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CLIENT_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.errorDescription").isNotEmpty())
                .andExpect(jsonPath("$.statusCode").value(409));

        long clientsWithMdmId = clientRepository
                .findAll()
                .stream()
                .filter(client -> mdmId.equals(client.getMdmId()))
                .count();

        assertEquals(1L, clientsWithMdmId);
    }

    @Test
    @DataSet(value = "datasets/client/active-client.yml",
            cleanBefore = true,
            cleanAfter = true,
            disableConstraints = true,
            skipCleaningFor = {
                "account_status", "databasechangelog", "databasechangeloglock"
            })
    void getClientShouldReturnPersistedClient() throws Exception {
        UUID clientId = UUID.fromString("11111111-1111-1111-1111-111111111111");


        mockMvc.perform(get("/api/v1/clients/{clientId}", clientId)
                        .accept(MediaType.APPLICATION_JSON))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(clientId.toString()))
                .andExpect(jsonPath("$.mdmId").value(8888888888L))
                .andExpect(jsonPath("$.firstName").value("Иван"))
                .andExpect(jsonPath("$.lastName").value("Петров"))
                .andExpect(jsonPath("$.middleName").value("Сергеевич"))
                .andExpect(jsonPath("$.citizenship").value("Россия"))
                .andExpect(jsonPath("$.clientType").value("INDIVIDUAL"))
                .andExpect(jsonPath("$.documentNumber").value("123456"))
                .andExpect(jsonPath("$.documentSeries").value("1234"))
                .andExpect(jsonPath("$.documentType").value("PASSPORT"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.hasAccounts").value(false));
    }

    private Client saveClient(Long mdmId) {
        return saveClient(
                mdmId,
                "Иван",
                "Петров"
        );
    }

    private Client saveClient(Long mdmId, String firstName, String lastName) {
        Client client = Client.builder()
                .mdmId(mdmId)
                .firstName(firstName)
                .lastName(lastName)
                .middleName("Сергеевич")
                .citizenship("Россия")
                .clientType("INDIVIDUAL")
                .documentNumber(String.valueOf(mdmId))
                .documentSeries("1234")
                .documentType("PASSPORT")
                .build();

        return clientRepository.saveAndFlush(client);
    }


    @Test
    void updateClientShouldUpdatePersistedClient() throws Exception {
        Client savedClient = saveClient(6666666666L);

        String originalDocumentNumber = savedClient.getDocumentNumber();

        UpdateClientRequestDto request = new UpdateClientRequestDto();

        request.setFirstName("Пётр");
        request.setLastName("Сидоров");
        request.setMiddleName("Александрович");

        mockMvc.perform(put("/api/v1/clients/{clientId}", savedClient.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedClient.getId().toString()))
                .andExpect(jsonPath("$.mdmId").value(6666666666L))
                .andExpect(jsonPath("$.firstName").value("Пётр"))
                .andExpect(jsonPath("$.lastName").value("Сидоров"))
                .andExpect(jsonPath("$.middleName").value("Александрович"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        Client updatedClient = clientRepository
                .findById(savedClient.getId())
                .orElseThrow();

        assertAll(
                () -> assertEquals("Пётр", updatedClient.getFirstName()),
                () -> assertEquals("Сидоров", updatedClient.getLastName()),
                () -> assertEquals("Александрович", updatedClient.getMiddleName()),
                () -> assertEquals(6666666666L, updatedClient.getMdmId()),
                () -> assertEquals(originalDocumentNumber, updatedClient.getDocumentNumber()),
                () -> assertEquals(ClientStatus.ACTIVE, updatedClient.getStatus()),
                () -> assertNotNull(updatedClient.getUpdatedAt())
        );
    }

    @Test
    void deleteClientShouldSoftDeleteClient() throws Exception {
        Client savedClient = saveClient(5555555555L);

        mockMvc.perform(delete("/api/v1/clients/{clientId}", savedClient.getId()))
                .andExpect(status().isNoContent());

        Client deletedClient = clientRepository
                .findById(savedClient.getId())
                .orElseThrow();

        assertEquals(
                ClientStatus.DELETED,
                deletedClient.getStatus()
        );

        mockMvc.perform(get("/api/v1/clients/{clientId}", savedClient.getId())
                        .accept(MediaType.APPLICATION_JSON))

                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CLIENT_NOT_FOUND"))
                .andExpect(jsonPath("$.statusCode").value(404));

        mockMvc.perform(get("/api/v1/clients/{clientId}/exists", savedClient.getId())
                        .accept(MediaType.APPLICATION_JSON))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false))
                .andExpect(jsonPath("$.clientId").value(savedClient.getId().toString()))
                .andExpect(jsonPath("$.status").value("DELETED"));
    }

    @Test
    @DataSet(value = "datasets/client/multiple-clients.yml",
            cleanBefore = true,
            cleanAfter = true,
            disableConstraints = true,
            skipCleaningFor = {
                    "account_status",
                    "databasechangelog",
                    "databasechangeloglock"
            }
    )
    void searchClientsShouldFilterByLastNameAndExcludeDeleted() throws Exception {
        mockMvc.perform(get("/api/v1/clients")
                        .param("page", "0")
                        .param("size", "20")
                        .param("lastName", "пЕтР")
                        .accept(MediaType.APPLICATION_JSON))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[*].mdmId").value(containsInAnyOrder(9111111111L, 9222222222L)))
                .andExpect(jsonPath("$.content[*].status").value(containsInAnyOrder("ACTIVE", "ACTIVE")))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.pageable.pageSize").value(20))
                .andExpect(jsonPath("$.pageable.totalElements").value(2))
                .andExpect(jsonPath("$.pageable.totalPages").value(1));
    }

    @Test
    void searchClientsShouldFilterByMdmId() throws Exception {
        saveClient(
                9555555555L,
                "Иван",
                "Петров"
        );

        Client expectedClient = saveClient(
                9666666666L,
                "Анна",
                "Сидорова"
        );

        saveClient(
                9777777777L,
                "Пётр",
                "Иванов"
        );

        mockMvc.perform(get("/api/v1/clients")
                        .param("page", "0")
                        .param("size", "20")
                        .param("mdmId", expectedClient.getMdmId().toString())
                        .accept(MediaType.APPLICATION_JSON))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(expectedClient.getId().toString()))
                .andExpect(jsonPath("$.content[0].mdmId").value(9666666666L))
                .andExpect(jsonPath("$.content[0].firstName").value("Анна"))
                .andExpect(jsonPath("$.content[0].lastName").value("Сидорова"))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.pageable.totalElements").value(1));
    }

    @Test
    void searchClientsShouldReturnRequestedPage() throws Exception {
        saveClient(
                9811111111L,
                "Иван",
                "Андреев"
        );

        saveClient(
                9822222222L,
                "Пётр",
                "Волков"
        );

        saveClient(
                9833333333L,
                "Анна",
                "Сидорова"
        );

        mockMvc.perform(get("/api/v1/clients")
                        .param("page", "1")
                        .param("size", "2")
                        .accept(MediaType.APPLICATION_JSON))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.pageable.pageNumber").value(1))
                .andExpect(jsonPath("$.pageable.pageSize").value(2))
                .andExpect(jsonPath("$.pageable.totalElements").value(3))
                .andExpect(jsonPath("$.pageable.totalPages").value(2));
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatusName.class, names = {
                    "NEW",
                    "IN_CREATION",
                    "CREATED"
            })
    void deleteClientShouldReturn409WhenAccountIsActive(AccountStatusName accountStatusName) throws Exception {
        Client savedClient = saveClient(5444444444L);

        AccountStatus accountStatus = accountStatusRepository
                .findByName(accountStatusName)
                .orElseThrow();

        Account account = new Account(
                accountStatus,
                savedClient,
                "CURRENT",
                "RUB"
        );

        accountRepository.saveAndFlush(account);

        mockMvc.perform(delete("/api/v1/clients/{clientId}", savedClient.getId())
                        .accept(MediaType.APPLICATION_JSON))

                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CLIENT_HAS_ACTIVE_ACCOUNTS"))
                .andExpect(jsonPath("$.errorDescription").isNotEmpty())
                .andExpect(jsonPath("$.statusCode").value(409));

        Client clientAfterDeleteAttempt = clientRepository
                .findById(savedClient.getId())
                .orElseThrow();

        assertEquals(
                ClientStatus.ACTIVE,
                clientAfterDeleteAttempt.getStatus()
        );

        assertTrue(
                accountRepository.existsById(account.getId())
        );
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatusName.class, names = {
                    "CANCELLED",
                    "CLOSED"
            })
    void deleteClientShouldSoftDeleteWhenAccountIsNotActive(AccountStatusName accountStatusName) throws Exception {
        Client savedClient = saveClient(5333333333L);

        AccountStatus accountStatus = accountStatusRepository
                .findByName(accountStatusName)
                .orElseThrow();

        Account account = new Account(
                accountStatus,
                savedClient,
                "CURRENT",
                "RUB"
        );

        accountRepository.saveAndFlush(account);

        mockMvc.perform(delete("/api/v1/clients/{clientId}", savedClient.getId()))
                .andExpect(status().isNoContent());

        Client deletedClient = clientRepository
                .findById(savedClient.getId())
                .orElseThrow();

        assertEquals(
                ClientStatus.DELETED,
                deletedClient.getStatus()
        );

        assertTrue(
                accountRepository.existsById(account.getId())
        );
    }


}