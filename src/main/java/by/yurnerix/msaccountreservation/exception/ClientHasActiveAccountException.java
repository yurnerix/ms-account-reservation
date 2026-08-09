package by.yurnerix.msaccountreservation.exception;

import java.util.UUID;

public class ClientHasActiveAccountException extends RuntimeException {

    public ClientHasActiveAccountException(UUID clientId)
    {
        super("Нельзя удалить клиента %s: у клиента есть активные счета".formatted(clientId));
    }

}
