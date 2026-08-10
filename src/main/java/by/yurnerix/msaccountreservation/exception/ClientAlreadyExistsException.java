package by.yurnerix.msaccountreservation.exception;

public class ClientAlreadyExistsException extends RuntimeException {

    public ClientAlreadyExistsException(Long mdmId) {
        super("Клиент с mdmId %d уже существует".formatted(mdmId));
    }

}
