package by.yurnerix.msaccountreservation.exception;

import java.time.Duration;
import java.util.UUID;

public class ClientReportTimeoutException extends RuntimeException {

    public ClientReportTimeoutException(UUID clientId, Duration timeout) {
        super("Client report generation timed out: " + "clientId=%s, timeout=%s".formatted(clientId, timeout));
    }
}
