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
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ClientMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Client toEntity(CreateClientRequestDto request);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "middleName", source = "middleName")
    void updateEntity(UpdateClientRequestDto request, @MappingTarget Client client);


    ClientResponseDto toResponse(Client client);

    @Mapping(target = "hasAccounts", constant = "false")
    ClientDetailsResponseDto toDetailsResponse(Client client);

    ClientSummaryDto toSummary(Client client);


    default ClientPageResponseDto toPageResponse(Page<Client> page) {
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

    default ClientExistsResponseDto toExistsResponse(UUID requestedClientId, Client client) {
        boolean exists = client != null && client.getStatus() != ClientStatus.DELETED;

        ClientExistsResponseDto response = new ClientExistsResponseDto()
                .exists(exists)
                .clientId(requestedClientId);

        if (client != null) {
            response.setStatus(toStatusDto(client.getStatus()));
        }

        return response;
    }


    default ClientStatusDto toStatusDto(ClientStatus status) {
        if (status == null) {
            return null;
        }

        return ClientStatusDto.fromValue(status.name());
    }
}