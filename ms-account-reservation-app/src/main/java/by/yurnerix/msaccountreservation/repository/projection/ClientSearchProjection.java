package by.yurnerix.msaccountreservation.repository.projection;

import by.yurnerix.msaccountreservation.entity.Client;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClientSearchProjection {

    private final Client client;

    private final Long activeAccountsCount;

}
