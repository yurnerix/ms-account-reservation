package by.yurnerix.msaccountreservation.mapper;

import by.yurnerix.msaccountreservation.entity.Account;
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
import by.yurnerix.msaccountreservation.generated.dto.AccountResponseDto;
import org.mapstruct.*;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ClientMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "accounts", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Client toEntity(CreateClientRequestDto request);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "middleName", source = "middleName")
    void updateEntity(UpdateClientRequestDto request, @MappingTarget Client client);


    ClientResponseDto toResponse(Client client);

    @Mapping(target = "hasAccounts", source = "accounts", qualifiedByName = "hasAccounts")
    @Mapping(target = "accounts", source = "accounts")
    ClientDetailsResponseDto toDetailsResponse(Client client);

    @Mapping(target = "status", source = "status.name")
    AccountResponseDto toAccountResponse(Account account);

    @IterableMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    List<AccountResponseDto> toAccountResponses(List<Account> accounts);

    @Named("hasAccounts")
    default boolean hasAccounts(List<Account> accounts) {
        return accounts != null && !accounts.isEmpty();
    }

    @Mapping(target = "activeAccountsCount", source = "accounts", qualifiedByName = "countActiveAccounts")
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

    @Named("countActiveAccounts")
    default long countActiveAccounts(List<Account> accounts) {
        if (accounts == null) {
            return 0L;
        }

        return accounts.stream()
                .filter(account -> account.getStatus().getName().isActive())
                .count();
    }
}