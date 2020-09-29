package com.mercadolibre.planning.model.me.exception;

import com.mercadolibre.planning.model.me.usecases.authorization.exceptions.UserNotAuthorizedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

/**
 * Handles exceptions thrown by the application and converts it into a proper API response.
 * Every exception thrown must be registered here with a proper handler.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final String EXCEPTION_ATTRIBUTE = "application.exception";

    @ExceptionHandler(UserNotAuthorizedException.class)
    public ResponseEntity<ErrorResponse> handle(final UserNotAuthorizedException exception,
                                                final HttpServletRequest request) {

        final ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.FORBIDDEN)
                .message(exception.getMessage())
                .error("user_not_authorized_error")
                .build();

        return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
    }

    @ExceptionHandler(ForecastParsingException.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception,
                                                         HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(exception.getMessage())
                .error("bad_request")
                .build();

        request.setAttribute(EXCEPTION_ATTRIBUTE, exception);
        return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
    }
}