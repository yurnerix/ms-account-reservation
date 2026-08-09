package by.yurnerix.msaccountreservation.mapper;

import by.yurnerix.msaccountreservation.entity.Client;
import by.yurnerix.msaccountreservation.entity.ClientStatus;
import by.yurnerix.msaccountreservation.generated.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


class ClientMapperTest {

    private ClientMapper clientMapper;

    private UUID clientId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updateAt;
    private Client client;


    @BeforeEach
    void setUp()
    {
        clientMapper = new ClientMapper();

        clientId = UUID.fromString("a1b2c3d4-e5f6-4890-abcd-ef1234567890");

        createdAt = OffsetDateTime.parse("2024-01-15T10:30:00Z");

        updateAt = OffsetDateTime.parse("2024-01-15T11:45:00Z");

        client = new Client();

        client.setId(clientId);
        client.setMdmId(1234567890L);
        client.setFirstName("Иван");
        client.setLastName("Петров");
        client.setMiddleName("Сергеевич");
        client.setCitizenship("Россия");
        client.setClientType("INDIVIDUAL");
        client.setDocumentNumber("123456");
        client.setDocumentSeries("1234");
        client.setDocumentType("PASSPORT");
        client.setStatus(ClientStatus.ACTIVE);
        client.setCreatedAt(createdAt);
        client.setUpdatedAt(updateAt);
    }

    @Test
    void toEntityShouldMapCreateRequestToClient()
    {
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

        Client result = clientMapper.toEntity(request);

        assertAll(
                () -> assertNull(result.getId()),
                () -> assertEquals(request.getMdmId(), result.getMdmId()),
                () -> assertEquals(request.getFirstName(), result.getFirstName()),
                () -> assertEquals(request.getLastName(), result.getLastName()),
                () -> assertEquals(request.getMiddleName(), result.getMiddleName()),
                () -> assertEquals(request.getCitizenship(), result.getCitizenship()),
                () -> assertEquals(request.getClientType(), result.getClientType()),
                () -> assertEquals(request.getDocumentNumber(), result.getDocumentNumber()),
                () -> assertEquals(request.getDocumentSeries(), result.getDocumentSeries()),
                () -> assertEquals(request.getDocumentType(), result.getDocumentType()),
                () -> assertEquals(ClientStatus.ACTIVE, result.getStatus())
        );

    }

    @Test
    void updateEntityShouldUpdateClientNames()
    {

        UpdateClientRequestDto request = new UpdateClientRequestDto();

        request.setFirstName("Пётр");
        request.setLastName("Сидоров");
        request.setMiddleName("Александрович");

        clientMapper.updateEntity(client, request);

        assertAll(
                () -> assertEquals("Пётр", client.getFirstName()),
                () -> assertEquals("Сидоров", client.getLastName()),
                () -> assertEquals("Александрович", client.getMiddleName()),
                () -> assertEquals(1234567890L, client.getMdmId()),
                () -> assertEquals(ClientStatus.ACTIVE, client.getStatus()),
                () -> assertEquals("123456", client.getDocumentNumber())
        );

    }

    @Test
    void toResponseShouldMapClientToResponseDto()
    {
        ClientResponseDto result = clientMapper.toResponse(client);

        assertAll(
                () -> assertEquals(clientId, result.getId()),
                () -> assertEquals(1234567890L, result.getMdmId()),
                () -> assertEquals("Иван", result.getFirstName()),
                () -> assertEquals("Петров", result.getLastName()),
                () -> assertEquals("Сергеевич", result.getMiddleName()),
                () -> assertEquals("Россия", result.getCitizenship()),
                () -> assertEquals("INDIVIDUAL", result.getClientType()),
                () -> assertEquals("123456", result.getDocumentNumber()),
                () -> assertEquals("1234", result.getDocumentSeries()),
                () -> assertEquals("PASSPORT", result.getDocumentType()),
                () -> assertEquals(ClientStatusDto.ACTIVE, result.getStatus()),
                () -> assertEquals(createdAt, result.getCreatedAt()),
                () -> assertEquals(updateAt, result.getUpdatedAt())
        );

    }

    @Test
    void toDetailsResponseShouldMapClientAndSetHasAccountsFalse()
    {
        ClientDetailsResponseDto result =
                clientMapper.toDetailsResponse(client);

        assertAll(
                () -> assertEquals(clientId, result.getId()),
                () -> assertEquals(1234567890L, result.getMdmId()),
                () -> assertEquals("Иван", result.getFirstName()),
                () -> assertEquals("Петров", result.getLastName()),
                () -> assertEquals("Сергеевич", result.getMiddleName()),
                () -> assertEquals("Россия", result.getCitizenship()),
                () -> assertEquals("INDIVIDUAL", result.getClientType()),
                () -> assertEquals("123456", result.getDocumentNumber()),
                () -> assertEquals("1234", result.getDocumentSeries()),
                () -> assertEquals("PASSPORT", result.getDocumentType()),
                () -> assertEquals(ClientStatusDto.ACTIVE, result.getStatus()),
                () -> assertEquals(createdAt, result.getCreatedAt()),
                () -> assertEquals(updateAt, result.getUpdatedAt()),
                () -> assertFalse(result.getHasAccounts())
        );
    }

    @Test
    void toPageResponseShouldMapContentAndPageMetadata()
    {
        Page<Client> page = new PageImpl<>(List.of(client), PageRequest.of(1, 2), 5);

        ClientPageResponseDto result = clientMapper.toPageResponse(page);

        assertNotNull(result.getContent());
        assertEquals(1, result.getContent().size());

        ClientSummaryDto summary = result.getContent().getFirst();

        assertAll(
                () -> assertEquals(clientId, summary.getId()),
                () -> assertEquals(1234567890L, summary.getMdmId()),
                () -> assertEquals("Иван", summary.getFirstName()),
                () -> assertEquals("Петров", summary.getLastName()),
                () -> assertEquals("Сергеевич", summary.getMiddleName()),
                () -> assertEquals(ClientStatusDto.ACTIVE, summary.getStatus())
        );

        PageMetadataDto metadata = result.getPageable();

        assertNotNull(metadata);

        assertAll(
                () -> assertEquals(1, metadata.getPageNumber()),
                () -> assertEquals(2, metadata.getPageSize()),
                () -> assertEquals(3, metadata.getTotalPages()),
                () -> assertEquals(5L, metadata.getTotalElements())
        );
    }

    @Test
    void toExistsResponseShouldReturnTrueForActiveClient()
    {
        ClientExistsResponseDto result = clientMapper.toExistsResponse(clientId, client);

        assertAll(
                () -> assertTrue(result.getExists()),
                () -> assertEquals(clientId, result.getClientId()),
                () -> assertEquals(ClientStatusDto.ACTIVE, result.getStatus())
        );
    }

    @Test
    void toExistsResponseShouldReturnFalseForDeletedClient()
    {
        client.setStatus(ClientStatus.DELETED);

        ClientExistsResponseDto result = clientMapper.toExistsResponse(clientId, client);

        assertAll(
                () -> assertFalse(result.getExists()),
                () -> assertEquals(clientId, result.getClientId()),
                () -> assertEquals(ClientStatusDto.DELETED, result.getStatus())
        );
    }

    @Test
    void toExistsResponseShouldReturnFalseWhenClientIsMissing()
    {
        ClientExistsResponseDto result = clientMapper.toExistsResponse(clientId, null);

        assertAll(
                () -> assertFalse(result.getExists()),
                () -> assertEquals(clientId, result.getClientId()),
                () -> assertNull(result.getStatus())
        );
    }
}