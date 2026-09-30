package by.yurnerix.msaccountreservation.specification;

import by.yurnerix.msaccountreservation.entity.Client;
import by.yurnerix.msaccountreservation.entity.ClientStatus;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientSpecifications {

    public static Specification<Client> forSearch(ClientStatus excludedStatus, String lastNamePattern, Long mdmId) {
        Specification<Client> specification = statusNot(excludedStatus);

        if (lastNamePattern != null) {
            specification = specification.and(lastNameLike(lastNamePattern));
        }

        if (mdmId != null) {
            specification = specification.and(mdmIdEquals(mdmId));
        }

        return specification;
    }

    public static Specification<Client> notDeleted() {
        return statusNot(ClientStatus.DELETED);
    }

    public static Specification<Client> statusNot(ClientStatus status) {
        return ((root, query, builder) ->
                builder.notEqual(root.get("status"), status));
    }

    public static Specification<Client> lastNameContains(String lastName) {
        String pattern = "%" + lastName.trim().toLowerCase(Locale.ROOT) + "%";

        return lastNameLike(pattern);
    }

    public static Specification<Client> mdmIdEquals(Long mdmId) {
        return (root, query, builder) ->
                builder.equal(root.get("mdmId"), mdmId);
    }

    private static Specification<Client> lastNameLike(String pattern) {
        return ((root, query, builder) ->
                builder.like(builder.lower(root.get("lastName")), pattern));
    }
}
