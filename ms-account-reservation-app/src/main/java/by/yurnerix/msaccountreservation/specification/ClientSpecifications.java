package by.yurnerix.msaccountreservation.specification;

import by.yurnerix.msaccountreservation.entity.Client;
import by.yurnerix.msaccountreservation.entity.ClientStatus;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientSpecifications {

    public static Specification<Client> notDeleted() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.notEqual(root.get("status"), ClientStatus.DELETED);
    }

    public static Specification<Client> lastNameContains(String lastName) {
        String normalizedLastName = lastName
                .trim()
                .toLowerCase(Locale.ROOT);

        String pattern = "%" + normalizedLastName + "%";

        return (root, query, criteriaBuilder) ->
                criteriaBuilder
                        .like(criteriaBuilder
                                .lower(root.get("lastName")), pattern);
    }

    public static Specification<Client> mdmIdEquals(Long mdmId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("mdmId"),
                        mdmId
                );
    }
}