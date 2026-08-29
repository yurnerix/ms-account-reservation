package by.yurnerix.msaccountreservation.exception;

import by.yurnerix.msaccountreservation.generated.dto.ErrorCodeDto;
import by.yurnerix.msaccountreservation.generated.dto.ErrorResponseDto;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleClientNotFound(ClientNotFoundException exception) {
        log.warn(
                "Client not found: {}",
                exception.getMessage()
        );

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                ErrorCodeDto.CLIENT_NOT_FOUND,
                exception.getMessage()
        );
    }

    @ExceptionHandler(ClientAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleClientAlreadyExists(ClientAlreadyExistsException exception) {
        log.warn(
                "Client already exists: {}",
                exception.getMessage()
        );

        return buildErrorResponse(
                HttpStatus.CONFLICT,
                ErrorCodeDto.CLIENT_ALREADY_EXISTS,
                exception.getMessage()
        );
    }

    @ExceptionHandler(ClientHasActiveAccountException.class)
    public ResponseEntity<ErrorResponseDto> handleClientHasActiveAccounts(ClientHasActiveAccountException exception) {
        log.warn(
                "Client deletion conflict: {}",
                exception.getMessage()
        );

        return buildErrorResponse(
                HttpStatus.CONFLICT,
                ErrorCodeDto.CLIENT_HAS_ACTIVE_ACCOUNTS,
                exception.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleRequestBodyValidation(MethodArgumentNotValidException exception) {
        String description = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                        "%s: %s".formatted(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                )
                .distinct()
                .collect(Collectors.joining("; "));

        log.warn(
                "Request body validation failed: {}",
                description
        );

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCodeDto.VALIDATION_ERROR,
                description
        );

    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodValidation(HandlerMethodValidationException exception) {
        log.warn(
                "Request parameter validation: {}",
                exception.getMessage()
        );

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCodeDto.VALIDATION_ERROR,
                "Параметры запроса не прошли валидацию"
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleConstraintViolation(ConstraintViolationException exception) {
        String description = exception.getConstraintViolations()
                .stream()
                .map(violation ->
                        "%s: %s".formatted(
                                violation.getPropertyPath(),
                                violation.getMessage()
                        )
                )
                .distinct()
                .collect(Collectors.joining("; "));

        log.warn(
                "Constraint validation failed: {}",
                description
        );

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCodeDto.VALIDATION_ERROR,
                description
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        String description = "Некорректное значение параметра: "
                + exception.getName();

        log.warn(
                "Request parameter type mismatch: parameter={}",
                exception.getName()
        );

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCodeDto.VALIDATION_ERROR,
                description
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleUnreadableRequest(HttpMessageNotReadableException exception) {
        log.warn(
                "Request body cannot be read: {}",
                exception.getMessage()
        );

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCodeDto.VALIDATION_ERROR,
                "Тело запроса содержит некорректный JSON"
        );
    }

    @ExceptionHandler(ClientReportTimeoutException.class)
    public ResponseEntity<ErrorResponseDto> handleClientReportTimeout(ClientReportTimeoutException exception) {
        log.warn(
                "Client report generation timed out: {}",
                exception.getMessage()
        );

        return buildErrorResponse(
                HttpStatus.GATEWAY_TIMEOUT,
                ErrorCodeDto.CLIENT_REPORT_TIMEOUT,
                "Превышено время ожидания формирования сводки клиента"
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleUnexpectedException(Exception exception) {
        log.error(
                "Unexpected application error",
                exception
        );

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCodeDto.INTERNAL_ERROR,
                "Произошла внутренняя ошибка приложения"
        );
    }

    private ResponseEntity<ErrorResponseDto> buildErrorResponse(HttpStatus status, ErrorCodeDto errorCode, String description) {
        ErrorResponseDto response = new ErrorResponseDto(errorCode, description, status.value());

        return ResponseEntity
                .status(status)
                .body(response);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponseDto> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        log.warn(
                "Unsupported content type: {}",
                exception.getContentType()
        );

        return buildErrorResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                ErrorCodeDto.VALIDATION_ERROR,
                "Поддерживается только Content-Type: application/json"
        );
    }
}
