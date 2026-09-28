package by.yurnerix.msaccountreservation.service;

import by.yurnerix.msaccountreservation.entity.AccountStatusName;
import by.yurnerix.msaccountreservation.entity.Client;
import by.yurnerix.msaccountreservation.entity.ClientStatus;
import by.yurnerix.msaccountreservation.exception.ClientAlreadyExistsException;
import by.yurnerix.msaccountreservation.exception.ClientHasActiveAccountException;
import by.yurnerix.msaccountreservation.exception.ClientNotFoundException;
import by.yurnerix.msaccountreservation.generated.dto.ClientDetailsResponseDto;
import by.yurnerix.msaccountreservation.generated.dto.ClientExistsResponseDto;
import by.yurnerix.msaccountreservation.generated.dto.ClientPageResponseDto;
import by.yurnerix.msaccountreservation.generated.dto.ClientResponseDto;
import by.yurnerix.msaccountreservation.generated.dto.CreateClientRequestDto;
import by.yurnerix.msaccountreservation.generated.dto.UpdateClientRequestDto;
import by.yurnerix.msaccountreservation.mapper.ClientMapper;
import by.yurnerix.msaccountreservation.repository.AccountRepository;
import by.yurnerix.msaccountreservation.repository.ClientRepository;
import by.yurnerix.msaccountreservation.repository.projection.ClientSearchProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientService {

    private static final Set<AccountStatusName> ACTIVE_ACCOUNT_STATUSES = AccountStatusName.activeStatuses();

    private final ClientRepository clientRepository;
    private final AccountRepository accountRepository;
    private final ClientMapper clientMapper;

    @Transactional
    public ClientResponseDto createClient(CreateClientRequestDto request) {
        Long mdmId = request.getMdmId();

        if (clientRepository.existsByMdmId(mdmId)) {
            throw new ClientAlreadyExistsException(mdmId);
        }

        Client client = clientMapper.toEntity(request);
        Client savedClient = clientRepository.saveAndFlush(client);

        log.info(
                "Client created successfully: clientId={}, mdmId={}",
                savedClient.getId(),
                savedClient.getMdmId()
        );

        return clientMapper.toResponse(savedClient);
    }

    @Transactional(readOnly = true)
    public ClientDetailsResponseDto getClient(UUID clientId) {
        Client client = clientRepository
                .findDetailsByIdAndStatusNot(clientId, ClientStatus.DELETED)
                .orElseThrow(() -> new ClientNotFoundException(clientId));

        return clientMapper.toDetailsResponse(client);
    }

    @Transactional(readOnly = true)
    public ClientPageResponseDto searchClients(int page, int size, String lastName, Long mdmId) {
        String lastNamePattern = StringUtils.hasText(lastName)
                ? "%" + lastName.trim().toLowerCase(Locale.ROOT) + "%"
                : null;


        Sort sort = Sort.by(
                Sort.Order.asc("lastName"),
                Sort.Order.asc("firstName"),
                Sort.Order.asc("id")
        );

        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<ClientSearchProjection> clients =
                clientRepository.searchWithActiveAccountsCount(
                        ClientStatus.DELETED,
                        ACTIVE_ACCOUNT_STATUSES,
                        lastNamePattern,
                        mdmId,
                        pageRequest
                );

        return clientMapper.toPageResponse(clients);
    }

    @Transactional
    public ClientResponseDto updateClient(UUID clientId, UpdateClientRequestDto request) {
        Client client = findActiveClient(clientId);

        clientMapper.updateEntity(request, client);

        Client updatedClient = clientRepository.saveAndFlush(client);

        log.info(
                "Client updated successfully: clientId={}",
                updatedClient.getId()
        );

        return clientMapper.toResponse(updatedClient);
    }

    @Transactional
    public void deleteClient(UUID clientId) {
        Client client = findActiveClient(clientId);

        boolean hasActiveAccounts = accountRepository.existsByClientIdAndStatusNameIn(clientId, ACTIVE_ACCOUNT_STATUSES);

        if (hasActiveAccounts) {
            log.warn(
                    "Client deletion rejected because active accounts exist: clientId={}",
                    clientId
            );

            throw new ClientHasActiveAccountException(clientId);
        }

        client.setStatus(ClientStatus.DELETED);
        clientRepository.save(client);

        log.info(
                "Client soft deleted successfully: clientId={}",
                clientId
        );
    }

    @Transactional(readOnly = true)
    public ClientExistsResponseDto checkClientExists(UUID clientId) {
        Client client = clientRepository
                .findById(clientId)
                .orElse(null);

        return clientMapper.toExistsResponse(clientId, client);
    }

    private Client findActiveClient(UUID clientId) {
        return clientRepository
                .findByIdAndStatusNot(clientId, ClientStatus.DELETED)
                .orElseThrow(
                        () -> new ClientNotFoundException(clientId)
                );
    }
}