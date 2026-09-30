package by.yurnerix.msaccountreservation.repository;

import by.yurnerix.msaccountreservation.entity.Client;
import by.yurnerix.msaccountreservation.entity.ClientStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

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
}
