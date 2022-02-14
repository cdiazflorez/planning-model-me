package com.mercadolibre.planning.model.me.exception;

import com.mercadolibre.planning.model.me.clients.rest.planningmodel.exception.ForecastNotFoundException;
import com.mercadolibre.planning.model.me.controller.backlog.exception.BacklogNotRespondingException;
import com.mercadolibre.planning.model.me.controller.backlog.exception.EmptyStateException;
import com.mercadolibre.planning.model.me.usecases.authorization.exceptions.UserNotAuthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

/**
 * Handles exceptions thrown by the application and converts it into a proper API response.
 * Every exception thrown must be registered here with a proper handler.
 */
@Slf4j
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
    public ResponseEntity<ErrorResponse> handleException(
            Exception exception,
            HttpServletRequest request) {

        log.error(exception.getMessage(), exception);
        return getBadRequestResponseEntity(exception, request);
    }

    @ExceptionHandler(UnmatchedWarehouseException.class)
    public ResponseEntity<ErrorResponse> handleUnmatchedWarehouseException(
            Exception exception,
            HttpServletRequest request) {

        log.error(exception.getMessage(), exception);
        return getBadRequestResponseEntity(exception, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameterException(
            final MissingServletRequestParameterException exception,
            final HttpServletRequest request) {

        final ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(exception.getMessage())
                .error("missing_parameter")
                .build();

        request.setAttribute(EXCEPTION_ATTRIBUTE, exception);
        log.error(exception.getMessage(), exception);
        return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
    }

    @ExceptionHandler(ForecastNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleForecastNotFoundException(
            final ForecastNotFoundException exception,
            final HttpServletRequest request) {

        final ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND)
                .message(exception.getMessage())
                .error("forecast_not_found")
                .build();

        request.setAttribute(EXCEPTION_ATTRIBUTE, exception);
        log.error(exception.getMessage(), exception);
        return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
    }

    @ExceptionHandler(EmptyStateException.class)
    public ResponseEntity<ErrorResponse> handleEmptyStateException(
            final EmptyStateException exception,
            final HttpServletRequest request) {

        final ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND)
                .message(exception.getMessage())
                .error("empty_state")
                .build();

        request.setAttribute(EXCEPTION_ATTRIBUTE, exception);
        log.error(exception.getMessage(), exception);
        return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
    }

    @ExceptionHandler(NoPlannedDataException.class)
    public ResponseEntity<ErrorResponse> handleNoPlannedDataException(
            final NoPlannedDataException exception,
            final HttpServletRequest request) {

        final ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND)
                .message(exception.getMessage())
                .error("no_planned_data_forecast_not_found")
                .build();

        request.setAttribute(EXCEPTION_ATTRIBUTE, exception);
        log.error(exception.getMessage(), exception);
        return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
    }

    @ExceptionHandler(NullValueAtCellException.class)
    public ResponseEntity<ErrorResponse> handleNullValueAtCellException(
            NullValueAtCellException exception,
            HttpServletRequest request) {

        log.error(exception.getMessage(), exception);
        return getBadRequestResponseEntity(exception, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            final Exception exception,
            final HttpServletRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .message(exception.getMessage())
                .error("unknown_error")
                .build();

        request.setAttribute(EXCEPTION_ATTRIBUTE, exception);

        log.error(exception.getMessage(), exception);
        return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
    }

    @ExceptionHandler(BacklogNotRespondingException.class)
    public ResponseEntity<ErrorResponse> handleBacklogNotRespondingException ( final BacklogNotRespondingException exception, HttpServletRequest request) {
        final ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .message(exception.getMessage())
            .error("unprocessable_entity")
            .build();

        return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
    }

    private ResponseEntity<ErrorResponse> getBadRequestResponseEntity(
            Exception exception,
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
