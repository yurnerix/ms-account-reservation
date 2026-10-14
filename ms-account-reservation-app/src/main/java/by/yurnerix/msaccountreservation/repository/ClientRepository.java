package by.yurnerix.msaccountreservation.repository;

import by.yurnerix.msaccountreservation.entity.AccountStatusName;
import by.yurnerix.msaccountreservation.entity.Client;
import by.yurnerix.msaccountreservation.entity.ClientStatus;
import by.yurnerix.msaccountreservation.repository.projection.ClientAccountCountProjection;
import by.yurnerix.msaccountreservation.repository.projection.ClientSearchProjection;
import by.yurnerix.msaccountreservation.specification.ClientSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public interface ClientRepository extends JpaRepository<Client, UUID>, JpaSpecificationExecutor<Client>, ClientSearchRepository {

    boolean existsByMdmId(Long mdmId);

    Optional<Client> findByIdAndStatusNot(UUID id, ClientStatus status);

    @Query("""
            select c
            from Client c
            left join fetch c.accounts a
            left join fetch a.status
            where c.id = :id
              and c.status <> :excludedStatus
            """)
    Optional<Client> findDetailsByIdAndStatusNot(
            @Param("id") UUID id,
            @Param("excludedStatus") ClientStatus excludedStatus
    );

    @Query("""
            select a.client.id as clientId,
                   count(a.id) as activeAccountsCount
            from Account a
            where a.client.id in :clientIds
              and a.status.name in :activeStatuses
            group by a.client.id
            """)
    List<ClientAccountCountProjection> countActiveAccountsByClientIds(
            @Param("clientIds") Collection<UUID> clientIds,
            @Param("activeStatuses") Collection<AccountStatusName> activeStatuses
    );

    @Override
    @Transactional(readOnly = true)
    default Page<ClientSearchProjection> searchWithActiveAccountsCount(
            ClientStatus excludedStatus,
            Collection<AccountStatusName> activeStatuses,
            String lastNamePattern,
            Long mdmId,
            Pageable pageable
    ) {
        Page<Client> clients = findAll(ClientSpecifications.forSearch(excludedStatus, lastNamePattern, mdmId), pageable);

        if (clients.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, clients.getTotalElements());
        }

        List<UUID> clientIds = clients.getContent()
                .stream()
                .map(Client::getId)
                .toList();

        // тут сделал один запрос для всей страницы, а не запрос для каждого клиента
        Map<UUID, Long> accountCounts = countActiveAccountsByClientIds(clientIds, activeStatuses)
                .stream()
                .collect(Collectors.toMap(ClientAccountCountProjection::getClientId, ClientAccountCountProjection::getActiveAccountsCount));

        return clients.map(client -> new ClientSearchProjection(
                client,
                accountCounts.getOrDefault(client.getId(), 0L)
        ));
    }
}
