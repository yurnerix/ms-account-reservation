package by.yurnerix.msaccountreservation.mapper;

import by.yurnerix.msaccountreservation.entity.Client;
import by.yurnerix.msaccountreservation.entity.ClientStatus;
import by.yurnerix.msaccountreservation.generated.dto.ClientDetailsResponseDto;
import by.yurnerix.msaccountreservation.generated.dto.ClientExistsResponseDto;
import by.yurnerix.msaccountreservation.generated.dto.ClientPageResponseDto;
import by.yurnerix.msaccountreservation.generated.dto.ClientResponseDto;
import by.yurnerix.msaccountreservation.generated.dto.ClientStatusDto;
import by.yurnerix.msaccountreservation.generated.dto.ClientSummaryDto;
import by.yurnerix.msaccountreservation.generated.dto.CreateClientRequestDto;
import by.yurnerix.msaccountreservation.generated.dto.PageMetadataDto;
import by.yurnerix.msaccountreservation.generated.dto.UpdateClientRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class ClientMapper {

    public Client toEntity(CreateClientRequestDto request)
    {
        return new Client(
                request.getMdmId(),
                request.getFirstName(),
                request.getLastName(),
                request.getMiddleName(),
                request.getCitizenship(),
                request.getClientType(),
                request.getDocumentNumber(),
                request.getDocumentSeries(),
                request.getDocumentType()
        );
    }

    public void updateEntity(Client client, UpdateClientRequestDto request)
    {
        client.setFirstName(request.getFirstName());
        client.setLastName(request.getLastName());
        client.setMiddleName(request.getMiddleName());
    }

    public ClientResponseDto toResponse(Client client)
    {
        return new ClientResponseDto()
                .id(client.getId())
                .mdmId(client.getMdmId())
                .firstName(client.getFirstName())
                .lastName(client.getLastName())
                .middleName(client.getMiddleName())
                .citizenship(client.getCitizenship())
                .clientType(client.getClientType())
                .documentNumber(client.getDocumentNumber())
                .documentSeries(client.getDocumentSeries())
                .documentType(client.getDocumentType())
                .status(toStatusDto(client.getStatus()))
                .createdAt(client.getCreatedAt())
                .updatedAt(client.getUpdatedAt());
    }

    public ClientDetailsResponseDto toDetailsResponse(Client client)
    {
        return new ClientDetailsResponseDto()
                .id(client.getId())
                .mdmId(client.getMdmId())
                .firstName(client.getFirstName())
                .lastName(client.getLastName())
                .middleName(client.getMiddleName())
                .citizenship(client.getCitizenship())
                .clientType(client.getClientType())
                .documentNumber(client.getDocumentNumber())
                .documentSeries(client.getDocumentSeries())
                .documentType(client.getDocumentType())
                .status(toStatusDto(client.getStatus()))
                .createdAt(client.getCreatedAt())
                .updatedAt(client.getUpdatedAt())
                .hasAccounts(false);
    }

    public ClientSummaryDto toSummary(Client client)
    {
        return new ClientSummaryDto()
                .id(client.getId())
                .mdmId(client.getMdmId())
                .firstName(client.getFirstName())
                .lastName(client.getLastName())
                .middleName(client.getMiddleName())
                .status(toStatusDto(client.getStatus()));
    }

    public ClientPageResponseDto toPageResponse(Page<Client> page)
    {
        List<ClientSummaryDto> content = page.getContent()
                .stream()
                .map(this::toSummary)
                .toList();

        PageMetadataDto metadata = new PageMetadataDto()
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements());

        return new ClientPageResponseDto()
                .content(content)
                .pageable(metadata);
    }

    public ClientExistsResponseDto toExistsResponse(UUID requestedClientId, Client client) {
        boolean exists = client != null && client.getStatus() != ClientStatus.DELETED;

        ClientExistsResponseDto response = new ClientExistsResponseDto()
                .exists(exists)
                .clientId(requestedClientId);

        if (client != null)
        {
            response.setStatus(toStatusDto(client.getStatus()));
        }

        return response;
    }

    private ClientStatusDto toStatusDto(ClientStatus status)
    {
        return ClientStatusDto.fromValue(status.name());
    }
}