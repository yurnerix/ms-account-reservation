package by.yurnerix.msaccountreservation.controller;

import by.yurnerix.msaccountreservation.exception.ClientAlreadyExistsException;
import by.yurnerix.msaccountreservation.exception.ClientHasActiveAccountException;
import by.yurnerix.msaccountreservation.exception.ClientNotFoundException;
import by.yurnerix.msaccountreservation.generated.dto.*;
import by.yurnerix.msaccountreservation.service.ClientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.endsWith;

@WebMvcTest(ClientController.class)
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClientService clientService;

    private UUID clientId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @BeforeEach
    void setUp() {
        clientId = UUID.fromString("a1b2c3d4-e5f6-4890-abcd-ef1234567890");

        createdAt = OffsetDateTime.parse("2024-01-15T10:30:00Z");

        updatedAt = OffsetDateTime.parse("2024-01-15T11:45:00Z");
    }

    private CreateClientRequestDto createValidRequest() {
        CreateClientRequestDto request = new CreateClientRequestDto();

        request.setMdmId(1234567890L);
        request.setFirstName("Иван");
        request.setLastName("Петров");
        request.setMiddleName("Сергеевич");
        request.setCitizenship("Россия");
        request.setClientType("INDIVIDUAL");
        request.setDocumentNumber("123456");
        request.setDocumentSeries("1234");
        request.setDocumentType("PASSPORT");

        return request;
    }

    private ClientResponseDto createClientResponse() {
        ClientResponseDto response = new ClientResponseDto();

        response.setId(clientId);
        response.setMdmId(1234567890L);
        response.setFirstName("Иван");
        response.setLastName("Петров");
        response.setMiddleName("Сергеевич");
        response.setCitizenship("Россия");
        response.setClientType("INDIVIDUAL");
        response.setDocumentNumber("123456");
        response.setDocumentSeries("1234");
        response.setDocumentType("PASSPORT");
        response.setStatus(ClientStatusDto.ACTIVE);
        response.setCreatedAt(createdAt);
        response.setUpdatedAt(updatedAt);

        return response;
    }


    @Test
    void createClientShouldReturn201AndCreateClient() throws Exception {
        CreateClientRequestDto request = createValidRequest();

        ClientResponseDto response = createClientResponse();

        when(clientService.createClient(any(CreateClientRequestDto.class)
        )).thenReturn(response);

        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))

                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/clients/" + clientId)))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(clientId.toString()))
                .andExpect(jsonPath("$.mdmId").value(1234567890L))
                .andExpect(jsonPath("$.firstName").value("Иван"))
                .andExpect(jsonPath("$.lastName").value("Петров"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").value("2024-01-15T10:30:00Z"))
                .andExpect(jsonPath("$.updatedAt").value("2024-01-15T11:45:00Z"));

        verify(clientService)
                .createClient(any(CreateClientRequestDto.class));
    }

    @Test
    void getClientShouldReturn200AndClient() throws Exception {
        ClientDetailsResponseDto response = new ClientDetailsResponseDto();

        response.setId(clientId);
        response.setMdmId(1234567890L);
        response.setFirstName("Иван");
        response.setLastName("Петров");
        response.setMiddleName("Сергеевич");
        response.setCitizenship("Россия");
        response.setClientType("INDIVIDUAL");
        response.setDocumentNumber("123456");
        response.setDocumentSeries("1234");
        response.setDocumentType("PASSPORT");
        response.setStatus(ClientStatusDto.ACTIVE);
        response.setCreatedAt(createdAt);
        response.setUpdatedAt(updatedAt);
        response.setHasAccounts(false);

        when(clientService.getClient(clientId))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/clients/{clientId}", clientId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(clientId.toString()))
                .andExpect(jsonPath("$.mdmId").value(1234567890L))
                .andExpect(jsonPath("$.firstName").value("Иван"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.hasAccounts").value(false));

        verify(clientService).getClient(clientId);

    }

    @Test
    void checkClientExistsShouldReturn200AndExistsTrue() throws Exception {
        ClientExistsResponseDto response = new ClientExistsResponseDto();

        response.setExists(true);
        response.setClientId(clientId);
        response.setStatus(ClientStatusDto.ACTIVE);

        when(clientService.checkClientExists(clientId))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/v1/clients/{clientId}/exists", clientId)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.clientId").value(clientId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(clientService).checkClientExists(clientId);
    }

    @Test
    void deleteClientShouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/v1/clients/{clientId}", clientId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(clientService).deleteClient(clientId);
    }

    @Test
    void createClientShouldReturn400WhenRequestIsInvalid() throws Exception {
        CreateClientRequestDto request = createValidRequest();

        request.setFirstName("   ");

        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))

                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errorDescription").isNotEmpty())
                .andExpect(jsonPath("$.statusCode").value(400));

        verifyNoInteractions(clientService);
    }

    @Test
    void createClientShouldReturn409WhenMdmIdAlreadyExists() throws Exception {
        CreateClientRequestDto request = createValidRequest();

        when(clientService.createClient(any(CreateClientRequestDto.class)))
                .thenThrow(new ClientAlreadyExistsException(request.getMdmId()));

        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))

                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value("CLIENT_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.errorDescription").isNotEmpty())
                .andExpect(jsonPath("$.statusCode").value(409));

        verify(clientService).createClient(any(CreateClientRequestDto.class));
    }

    @Test
    void getClientShouldReturn404WhenClientDoesNotExist() throws Exception {
        when(clientService.getClient(clientId))
                .thenThrow(new ClientNotFoundException(clientId));

        mockMvc.perform(get("/api/v1/clients/{clientId}", clientId)
                        .accept(MediaType.APPLICATION_JSON))

                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value("CLIENT_NOT_FOUND"))
                .andExpect(jsonPath("$.errorDescription").isNotEmpty())
                .andExpect(jsonPath("$.statusCode").value(404));

        verify(clientService).getClient(clientId);
    }

    @Test
    void deleteClientShouldReturn409WhenClientHasActiveAccounts() throws Exception {
        doThrow(new ClientHasActiveAccountException(clientId))
                .when(clientService)
                .deleteClient(clientId);

        mockMvc.perform(delete("/api/v1/clients/{clientId}", clientId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value("CLIENT_HAS_ACTIVE_ACCOUNTS"))
                .andExpect(jsonPath("$.errorDescription").isNotEmpty())
                .andExpect(jsonPath("$.statusCode").value(409));

        verify(clientService).deleteClient(clientId);
    }

    private UpdateClientRequestDto createValidUpdateRequest() {
        UpdateClientRequestDto request = new UpdateClientRequestDto();

        request.setFirstName("Пётр");
        request.setLastName("Сидоров");
        request.setMiddleName("Александрович");

        return request;
    }

    @Test
    void updateClientShouldReturn200AndUpdatedClient() throws Exception {
        UpdateClientRequestDto request = createValidUpdateRequest();

        ClientResponseDto response = createClientResponse();

        response.setFirstName("Пётр");
        response.setLastName("Сидоров");
        response.setMiddleName("Александрович");

        when(clientService.updateClient(eq(clientId), any(UpdateClientRequestDto.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/clients/{clientId}", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(clientId.toString()))
                .andExpect(jsonPath("$.mdmId").value(1234567890L))
                .andExpect(jsonPath("$.firstName").value("Пётр"))
                .andExpect(jsonPath("$.lastName").value("Сидоров"))
                .andExpect(jsonPath("$.middleName").value("Александрович"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.updatedAt").value("2024-01-15T11:45:00Z"));

        verify(clientService)
                .updateClient(eq(clientId), any(UpdateClientRequestDto.class)
                );
    }

    @Test
    void updateClientShouldReturn400WhenRequestIsInvalid() throws Exception {
        UpdateClientRequestDto request = createValidUpdateRequest();

        request.setLastName(null);

        mockMvc.perform(put("/api/v1/clients/{clientId}", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errorDescription").isNotEmpty())
                .andExpect(jsonPath("$.statusCode").value(400));

        verifyNoInteractions(clientService);
    }

    @Test
    void updateClientShouldReturn404WhenClientDoesNotExist() throws Exception {
        UpdateClientRequestDto request = createValidUpdateRequest();

        when(clientService.updateClient(eq(clientId), any(UpdateClientRequestDto.class)))
                .thenThrow(new ClientNotFoundException(clientId));

        mockMvc.perform(put("/api/v1/clients/{clientId}", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))

                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value("CLIENT_NOT_FOUND"))
                .andExpect(jsonPath("$.errorDescription").isNotEmpty())
                .andExpect(jsonPath("$.statusCode").value(404));

        verify(clientService)
                .updateClient(eq(clientId), any(UpdateClientRequestDto.class));
    }


    private ClientPageResponseDto createPageResponse() {
        ClientSummaryDto summary = new ClientSummaryDto();

        summary.setId(clientId);
        summary.setMdmId(1234567890L);
        summary.setFirstName("Иван");
        summary.setLastName("Петров");
        summary.setMiddleName("Сергеевич");
        summary.setStatus(ClientStatusDto.ACTIVE);

        PageMetadataDto metadata = new PageMetadataDto();

        metadata.setPageNumber(0);
        metadata.setPageSize(20);
        metadata.setTotalPages(1);
        metadata.setTotalElements(1L);

        ClientPageResponseDto response = new ClientPageResponseDto();

        response.setContent(List.of(summary));
        response.setPageable(metadata);

        return response;
    }

    @Test
    void searchClientsShouldReturn200AndClientPage() throws Exception {
        ClientPageResponseDto response = createPageResponse();

        when(clientService.searchClients(
                0,
                20,
                "Петр",
                1234567890L
        )).thenReturn(response);

        mockMvc.perform(get("/api/v1/clients")
                        .param("page", "0")
                        .param("size", "20")
                        .param("lastName", "Петр")
                        .param("mdmId", "1234567890")
                        .accept(MediaType.APPLICATION_JSON))

                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(clientId.toString()))
                .andExpect(jsonPath("$.content[0].mdmId").value(1234567890L))
                .andExpect(jsonPath("$.content[0].firstName").value("Иван"))
                .andExpect(jsonPath("$.content[0].lastName").value("Петров"))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.pageable.pageSize").value(20))
                .andExpect(jsonPath("$.pageable.totalPages").value(1))
                .andExpect(jsonPath("$.pageable.totalElements").value(1));

        verify(clientService).searchClients(
                0,
                20,
                "Петр",
                1234567890L
        );
    }

    @Test
    void searchClientsShouldUseDefaultPagination() throws Exception {
        PageMetadataDto metadata = new PageMetadataDto();

        metadata.setPageNumber(0);
        metadata.setPageSize(20);
        metadata.setTotalPages(0);
        metadata.setTotalElements(0L);

        ClientPageResponseDto response = new ClientPageResponseDto();

        response.setContent(List.of());
        response.setPageable(metadata);

        when(clientService.searchClients(
                0,
                20,
                null,
                null
        )).thenReturn(response);

        mockMvc.perform(get("/api/v1/clients")
                        .accept(MediaType.APPLICATION_JSON))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.pageable.pageSize").value(20))
                .andExpect(jsonPath("$.pageable.totalElements").value(0));

        verify(clientService).searchClients(
                0,
                20,
                null,
                null
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 101})
    void searchClientsShouldReturn400WhenSizeIsInvalid(int invalidSize) throws Exception {
        mockMvc.perform(get("/api/v1/clients")
                        .param("page", "0")
                        .param("size", String.valueOf(invalidSize))
                        .accept(MediaType.APPLICATION_JSON))

                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errorDescription").isNotEmpty())
                .andExpect(jsonPath("$.statusCode").value(400));

        verifyNoInteractions(clientService);
    }

    @Test
    void searchClientsShouldReturn400WhenPageIsNegative() throws Exception {
        mockMvc.perform(
                        get("/api/v1/clients")
                                .param("page", "-1")
                                .param("size", "20")
                                .accept(MediaType.APPLICATION_JSON))

                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errorDescription").isNotEmpty())
                .andExpect(jsonPath("$.statusCode").value(400));

        verifyNoInteractions(clientService);
    }

    @Test
    void getClientShouldReturn400WhenClientIdIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/clients/{clientId}", "not-a-uuid")
                        .accept(MediaType.APPLICATION_JSON))

                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errorDescription").isNotEmpty())
                .andExpect(jsonPath("$.statusCode").value(400));

        verifyNoInteractions(clientService);
    }

    @Test
    void createClientShouldReturn400WhenJsonIsMalformed() throws Exception {
        String malformedJson = """
                {
                  "mdmId": 1234567890,
                  "firstName": "Иван",
                """;

        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(malformedJson))

                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errorDescription").isNotEmpty())
                .andExpect(jsonPath("$.statusCode").value(400));

        verifyNoInteractions(clientService);
    }

    @Test
    void createClientShouldReturn415WhenContentTypeIsUnsupported() throws Exception {
        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.TEXT_PLAIN)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("client data"))

                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.statusCode").value(415));

        verifyNoInteractions(clientService);
    }

    @Test
    void deleteClientShouldReturn404WhenClientDoesNotExist() throws Exception {
        doThrow(new ClientNotFoundException(clientId))
                .when(clientService)
                .deleteClient(clientId);

        mockMvc.perform(delete("/api/v1/clients/{clientId}", clientId)
                        .accept(MediaType.APPLICATION_JSON))

                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value("CLIENT_NOT_FOUND"))
                .andExpect(jsonPath("$.errorDescription").isNotEmpty())
                .andExpect(jsonPath("$.statusCode").value(404));

        verify(clientService).deleteClient(clientId);
    }

    @Test
    void getClientShouldReturn500WhenUnexpectedErrorOccurs()
            throws Exception {
        when(clientService.getClient(clientId))
                .thenThrow(new IllegalStateException("Unexpected test error"));

        mockMvc.perform(get("/api/v1/clients/{clientId}", clientId)
                        .accept(MediaType.APPLICATION_JSON))

                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.errorDescription").isNotEmpty())
                .andExpect(jsonPath("$.statusCode").value(500));

        verify(clientService).getClient(clientId);
    }

}
