package dev.kayquemiguel.aegis.observability;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> statusException(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {
        int status = exception.getStatusCode().value();

        String code;
        String message;

        if (status == 400) {
            code = "BAD_REQUEST";
            message = "Invalid request";
        } else if (status == 404) {
            code = "NOT_FOUND";
            message = "Resource not found";
        } else {
            code = "REQUEST_ERROR";
            message = "Request could not be completed";
        }

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(error(code, message, request));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiErrorResponse> invalidRequest(
            Exception exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error(
                        "BAD_REQUEST",
                        "Invalid request",
                        request
                ));
    }

    private static ApiErrorResponse error(
            String code,
            String message,
            HttpServletRequest request
    ) {
        return new ApiErrorResponse(
                code,
                message,
                CorrelationIdFilter.from(request)
        );
    }
}
