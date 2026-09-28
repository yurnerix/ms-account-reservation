package by.yurnerix.msaccountreservation.repository;

import by.yurnerix.msaccountreservation.entity.AccountStatusName;
import by.yurnerix.msaccountreservation.entity.Client;
import by.yurnerix.msaccountreservation.entity.ClientStatus;
import by.yurnerix.msaccountreservation.repository.projection.ClientSearchProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID>, JpaSpecificationExecutor<Client> {

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
    Optional<Client> findDetailsByIdAndStatusNot(@Param("id") UUID id, @Param("excludedStatus") ClientStatus excludedStatus);

    @Query(value = """
                           select c as client,
                           sum(
                               case
                                   when s.name in :activeStatuses then 1L
                                   else 0L
                               end
                           ) as activeAccountsCount
                    from Client c
                    left join c.accounts a
                    left join a.status s
                    where c.status <> :excludedStatus
                      and (
                          :lastNamePattern is null
                          or lower(c.lastName) like :lastNamePattern
                      )
                      and (
                          :mdmId is null
                          or c.mdmId = :mdmId
                      )
                    group by c
                    """,
            countQuery = """
                    select count(c.id)
                    from Client c
                    where c.status <> :excludedStatus
                      and (
                          :lastNamePattern is null
                          or lower(c.lastName) like :lastNamePattern
                      )
                      and (
                          :mdmId is null
                          or c.mdmId = :mdmId
                      )
                    """
    )
    Page<ClientSearchProjection> searchWithActiveAccountsCount(@Param("excludedStatus") ClientStatus excludedStatus,
                                                               @Param("activeStatuses") Collection<AccountStatusName> activeStatuses,
                                                               @Param("lastNamePattern") String lastNamePattern,
                                                               @Param("mdmId") Long mdmId,
                                                               Pageable pageable
    );
}
