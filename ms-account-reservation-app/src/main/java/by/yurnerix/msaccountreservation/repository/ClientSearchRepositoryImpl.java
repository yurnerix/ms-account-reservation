package by.yurnerix.msaccountreservation.repository;

import by.yurnerix.msaccountreservation.entity.Account;
import by.yurnerix.msaccountreservation.entity.AccountStatus;
import by.yurnerix.msaccountreservation.entity.AccountStatusName;
import by.yurnerix.msaccountreservation.entity.Client;
import by.yurnerix.msaccountreservation.entity.ClientStatus;
import by.yurnerix.msaccountreservation.repository.projection.ClientSearchProjection;
import by.yurnerix.msaccountreservation.specification.ClientSpecifications;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientSearchRepositoryImpl implements ClientSearchRepository {

    private final EntityManager entityManager;

    @Override
    public Page<ClientSearchProjection> searchWithActiveAccountsCount(ClientStatus excludedStatus, Collection<AccountStatusName> activeStatuses, String lastNamePattern, Long mdmId, Pageable pageable) {
        Specification<Client> specification = ClientSpecifications.forSearch(excludedStatus, lastNamePattern, mdmId);

        List<ClientSearchProjection> content = findContent(specification, activeStatuses, pageable);

        return PageableExecutionUtils.getPage(content, pageable, () -> countClients(specification));
    }

    private List<ClientSearchProjection> findContent(Specification<Client> specification, Collection<AccountStatusName> activeStatuses, Pageable pageable) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        CriteriaQuery<ClientSearchProjection> query = builder.createQuery(ClientSearchProjection.class);

        Root<Client> client = query.from(Client.class);

        Expression<Long> activeAccountsCount = countActiveAccounts(client, builder, activeStatuses);

        query.select(builder.construct(ClientSearchProjection.class, client, activeAccountsCount));

        query.where(specification.toPredicate(client, query, builder));
        query.groupBy(client);
        query.orderBy(QueryUtils.toOrders(pageable.getSort(), client, builder));

        TypedQuery<ClientSearchProjection> typedQuery = entityManager.createQuery(query);

        if (pageable.isPaged()) {
            typedQuery.setFirstResult(Math.toIntExact(pageable.getOffset()));
            typedQuery.setMaxResults(pageable.getPageSize());
        }

        return typedQuery.getResultList();
    }

    private Expression<Long> countActiveAccounts(Root<Client> client, CriteriaBuilder builder, Collection<AccountStatusName> activeStatuses) {
        Join<Client, Account> account = client.join("accounts", JoinType.LEFT);

        Join<Account, AccountStatus> status = account.join("status", JoinType.LEFT);

        status.on(status.get("name").in(activeStatuses));

        return builder.count(status.get("id"));
    }

    private long countClients(Specification<Client> specification) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        CriteriaQuery<Long> query = builder.createQuery(Long.class);

        Root<Client> client = query.from(Client.class);

        query.select(builder.count(client));
        query.where(specification.toPredicate(client, query, builder));

        return entityManager.createQuery(query).getSingleResult();
    }
}
