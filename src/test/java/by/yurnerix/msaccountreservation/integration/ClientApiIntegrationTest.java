package by.yurnerix.msaccountreservation.integration;

import by.yurnerix.msaccountreservation.entity.Client;
import by.yurnerix.msaccountreservation.entity.ClientStatus;
import by.yurnerix.msaccountreservation.generated.dto.ClientResponseDto;
import by.yurnerix.msaccountreservation.generated.dto.CreateClientRequestDto;
import by.yurnerix.msaccountreservation.generated.dto.UpdateClientRequestDto;
import by.yurnerix.msaccountreservation.repository.AccountStatusRepository;
import by.yurnerix.msaccountreservation.repository.ClientRepository;
import by.yurnerix.msaccountreservation.entity.Account;
import by.yurnerix.msaccountreservation.entity.AccountStatus;
import by.yurnerix.msaccountreservation.repository.AccountRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.hamcrest.Matchers.containsInAnyOrder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ClientApiIntegrationTest
{

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
    void contextShouldStartWithMigratedDatabase()
    {
        assertAll(
                () -> assertEquals(5, accountStatusRepository.count()),
                () -> assertTrue(accountStatusRepository.findByName("NEW").isPresent()),
                () -> assertTrue(accountStatusRepository.findByName("IN_CREATION").isPresent()),
                () -> assertTrue(accountStatusRepository.findByName("CREATED").isPresent()),
                () -> assertTrue(accountStatusRepository.findByName("CANCELLED").isPresent()),
                () -> assertTrue(accountStatusRepository.findByName("CLOSED").isPresent())
        );
    }

    @Test
    void createClientShouldPersistClientAndReturn201() throws Exception
    {
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

    private CreateClientRequestDto createValidRequest(Long mdmId)
    {
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
    void createClientShouldReturn409WhenMdmIdAlreadyExists() throws Exception
    {
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
    void getClientShouldReturnPersistedClient() throws Exception
    {
        Client client = new Client(
                8888888888L,
                "Иван",
                "Петров",
                "Сергеевич",
                "Россия",
                "INDIVIDUAL",
                "123456",
                "1234",
                "PASSPORT"
        );

        Client savedClient = clientRepository.saveAndFlush(client);

        mockMvc.perform(get("/api/v1/clients/{clientId}", savedClient.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(savedClient.getId().toString()))
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

    private Client saveClient(Long mdmId)
    {
        return saveClient(
                mdmId,
                "Иван",
                "Петров"
        );
    }

    private Client saveClient(Long mdmId, String firstName, String lastName)
    {
        Client client = new Client(
                mdmId,
                firstName,
                lastName,
                "Сергеевич",
                "Россия",
                "INDIVIDUAL",
                String.valueOf(mdmId),
                "1234",
                "PASSPORT"
        );

        return clientRepository.saveAndFlush(client);
    }


    @Test
    void updateClientShouldUpdatePersistedClient() throws Exception
    {
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
    void deleteClientShouldSoftDeleteClient() throws Exception
    {
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
    void searchClientsShouldFilterByLastNameAndExcludeDeleted() throws Exception
    {
        Client firstClient = saveClient(
                9111111111L,
                "Иван",
                "Петров"
        );

        Client secondClient = saveClient(
                9222222222L,
                "Пётр",
                "Петровский"
        );

        saveClient(
                9333333333L,
                "Анна",
                "Сидорова"
        );

        Client deletedClient = saveClient(
                9444444444L,
                "Алексей",
                "Петров"
        );

        deletedClient.setStatus(ClientStatus.DELETED);
        clientRepository.saveAndFlush(deletedClient);

        mockMvc.perform(get("/api/v1/clients")
                        .param("page", "0")
                        .param("size", "20")
                        .param("lastName", "пЕтР")
                        .accept(MediaType.APPLICATION_JSON))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[*].mdmId").value(containsInAnyOrder(firstClient.getMdmId(), secondClient.getMdmId())))
                .andExpect(jsonPath("$.content[*].status").value(containsInAnyOrder("ACTIVE", "ACTIVE")))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.pageable.pageSize").value(20))
                .andExpect(jsonPath("$.pageable.totalElements").value(2))
                .andExpect(jsonPath("$.pageable.totalPages").value(1));
    }

    @Test
    void searchClientsShouldFilterByMdmId() throws Exception
    {
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
    void searchClientsShouldReturnRequestedPage() throws Exception
    {
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
    @ValueSource(strings = {"NEW", "IN_CREATION", "CREATED"})
    void deleteClientShouldReturn409WhenAccountIsActive(String accountStatusName) throws Exception
    {
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
    @ValueSource(strings = {"CANCELLED", "CLOSED"})
    void deleteClientShouldSoftDeleteWhenAccountIsNotActive(String accountStatusName) throws Exception
    {
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