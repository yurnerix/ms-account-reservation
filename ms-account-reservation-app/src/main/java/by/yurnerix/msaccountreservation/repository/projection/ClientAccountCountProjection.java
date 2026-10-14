package by.yurnerix.msaccountreservation.repository.projection;

import java.util.UUID;

public interface ClientAccountCountProjection {

    UUID getClientId();

    Long getActiveAccountsCount();
}
