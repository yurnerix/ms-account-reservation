package by.yurnerix.msaccountreservation.repository;


import by.yurnerix.msaccountreservation.entity.AccountStatusName;
import by.yurnerix.msaccountreservation.entity.ClientStatus;
import by.yurnerix.msaccountreservation.repository.projection.ClientSearchProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;

public interface ClientSearchRepository {

    Page<ClientSearchProjection> searchWithActiveAccountsCount(ClientStatus excludedStatus, Collection<AccountStatusName> activeStatuses, String lastNamePattern, Long mdmId, Pageable pageable);
}
