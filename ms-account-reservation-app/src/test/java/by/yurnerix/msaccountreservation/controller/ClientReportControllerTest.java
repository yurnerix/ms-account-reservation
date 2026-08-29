package by.yurnerix.msaccountreservation.controller;

import by.yurnerix.msaccountreservation.exception.ClientReportTimeoutException;
import by.yurnerix.msaccountreservation.generated.dto.ClientDetailsResponseDto;
import by.yurnerix.msaccountreservation.generated.dto.ClientReportResponseDto;
import by.yurnerix.msaccountreservation.generated.dto.ClientStatusDto;
import by.yurnerix.msaccountreservation.service.ClientReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientReportController.class)
class ClientReportControllerTest {

    private static final UUID CLIENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientReportService clientReportService;

    @Test
    void getClientReportShouldReturn200AndReport() throws Exception {
        ClientDetailsResponseDto client = createClientDetailsResponse();

        Map<String, BigDecimal> exchangeRates = new LinkedHashMap<>();

        exchangeRates.put("USD/RUB", new BigDecimal("91.25"));
        exchangeRates.put("EUR/RUB", new BigDecimal("99.40"));

        ClientReportResponseDto response = new ClientReportResponseDto()
                        .client(client)
                        .exchangeRates(exchangeRates);

        when(clientReportService.getClientReport(CLIENT_ID))
                .thenReturn(CompletableFuture.completedFuture(response));

        MvcResult mvcResult = mockMvc
                .perform(get("/api/v1/clients/{clientId}/report", CLIENT_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.client.id")
                        .value(CLIENT_ID.toString()))
                .andExpect(jsonPath("$.client.firstName")
                        .value("Иван"))
                .andExpect(jsonPath("$.client.lastName")
                        .value("Петров"))
                .andExpect(jsonPath("$.client.status")
                        .value("ACTIVE"))
                .andExpect(jsonPath("$.exchangeRates['USD/RUB']")
                        .value(91.25))
                .andExpect(jsonPath("$.exchangeRates['EUR/RUB']")
                        .value(99.40));

        verify(clientReportService).getClientReport(CLIENT_ID);
    }

    @Test
    void getClientReportShouldReturn504WhenTimeoutOccurs() throws Exception {
        CompletableFuture<ClientReportResponseDto> failedFuture = CompletableFuture.failedFuture(new ClientReportTimeoutException(CLIENT_ID, Duration.ofMillis(100)));

        when(clientReportService.getClientReport(CLIENT_ID))
                .thenReturn(failedFuture);

        MvcResult mvcResult = mockMvc
                .perform(get("/api/v1/clients/{clientId}/report", CLIENT_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isGatewayTimeout())
                .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode")
                        .value("CLIENT_REPORT_TIMEOUT"))
                .andExpect(jsonPath("$.errorDescription")
                        .value("Превышено время ожидания " + "формирования сводки клиента"))
                .andExpect(jsonPath("$.statusCode")
                        .value(504));

        verify(clientReportService).getClientReport(CLIENT_ID);
    }

    @Test
    void getClientReportShouldReturn400WhenClientIdIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/clients/{clientId}/report", "not-a-uuid")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode")
                        .value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errorDescription")
                        .isNotEmpty())
                .andExpect(jsonPath("$.statusCode")
                        .value(400));

        verifyNoInteractions(clientReportService);
    }
    private ClientDetailsResponseDto createClientDetailsResponse() {
        ClientDetailsResponseDto client = new ClientDetailsResponseDto();

        client.setId(CLIENT_ID);
        client.setMdmId(1234567890L);
        client.setFirstName("Иван");
        client.setLastName("Петров");
        client.setMiddleName("Сергеевич");
        client.setCitizenship("Россия");
        client.setClientType("INDIVIDUAL");
        client.setDocumentType("PASSPORT");
        client.setDocumentSeries("1234");
        client.setDocumentNumber("123456");
        client.setStatus(ClientStatusDto.ACTIVE);
        client.setHasAccounts(false);

        return client;
    }
}
