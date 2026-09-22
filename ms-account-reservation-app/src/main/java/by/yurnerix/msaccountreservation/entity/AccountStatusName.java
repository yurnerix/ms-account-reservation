package by.yurnerix.msaccountreservation.entity;

import java.util.Set;

public enum AccountStatusName {
    NEW,
    IN_CREATION,
    CREATED,
    CANCELLED,
    CLOSED;

    private static final Set<AccountStatusName> ACTIVE_STATUSES = Set.of(
            NEW,
            IN_CREATION,
            CREATED
    );

    public boolean isActive() {
        return ACTIVE_STATUSES.contains(this);
    }

    public static Set<AccountStatusName> activeStatuses() {
        return ACTIVE_STATUSES;
    }
}
