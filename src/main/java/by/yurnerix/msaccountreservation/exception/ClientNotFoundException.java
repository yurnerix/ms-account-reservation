package by.yurnerix.msaccountreservation.exception;

import java.util.UUID;

public class ClientNotFoundException extends RuntimeException {

    public ClientNotFoundException(UUID clientId) {
        super("Клиент с идентификатором %s не найден".formatted(clientId));
    }
}
