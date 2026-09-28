package by.yurnerix.msaccountreservation.repository.projection;

import by.yurnerix.msaccountreservation.entity.Client;

public interface ClientSearchProjection {

    Client getClient();

    Long getActiveAccountsCount();

}
