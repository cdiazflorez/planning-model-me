package com.mercadolibre.planning.model.me.exception;

import com.mercadolibre.planning.model.me.clients.rest.planningmodel.exception.ForecastNotFoundException;
import com.mercadolibre.planning.model.me.controller.backlog.exception.BacklogNotRespondingException;
import com.mercadolibre.planning.model.me.controller.backlog.exception.EmptyStateException;
import com.mercadolibre.planning.model.me.usecases.authorization.exceptions.UserNotAuthorizedException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Handles exceptions thrown by the application and converts it into a proper API response.
 * Every exception thrown must be registered here with a proper handler.
 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

  private static final String EXCEPTION_ATTRIBUTE = "application.exception";

  private static final String BAD_REQUEST_ERROR = "bad_request";

  @ExceptionHandler(UserNotAuthorizedException.class)
  public ResponseEntity<ErrorResponse> handle(final UserNotAuthorizedException exception,
                                              final HttpServletRequest request) {

    final ErrorResponse errorResponse = ErrorResponse.builder()
        .status(HttpStatus.FORBIDDEN)
        .message(exception.getMessage())
        .error("user_not_authorized_error")
        .build();

    request.setAttribute(EXCEPTION_ATTRIBUTE, exception);
    log.error(exception.getMessage(), exception);
    return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
  }

  @ExceptionHandler(ForecastParsingException.class)
  public ResponseEntity<ErrorResponse> handleException(
      Exception exception,
      HttpServletRequest request) {

    log.error(exception.getMessage(), exception);
    return getBadRequestResponseEntity(exception, request);
  }

  @ExceptionHandler(UnitsPerOrderRatioException.class)
  public ResponseEntity<ErrorResponse> handleUnitsPerOrderRatioException(
      Exception exception,
      HttpServletRequest request) {

    log.error(exception.getMessage(), exception);
    return getBadRequestResponseEntity(exception, request);
  }

  @ExceptionHandler(LowerAndUpperLimitsException.class)
  public ResponseEntity<ErrorResponse> handleLowerAndUpperLimitsException(
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

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
      final IllegalArgumentException exception,
      final HttpServletRequest request) {

    log.error(exception.getMessage(), exception);
    return getBadRequestResponseEntity(exception, request);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleIllegalConstraintViolationException(
      final ConstraintViolationException exception,
      final HttpServletRequest request) {

    log.error(exception.getMessage(), exception);
    return getBadRequestResponseEntity(exception, request);
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

  @ExceptionHandler(InvalidParamException.class)
  public ResponseEntity<ErrorResponse> handleInvalidParamException(
      final InvalidParamException exception,
      final HttpServletRequest request) {

    log.error(exception.getMessage(), exception);
    return getBadRequestResponseEntity(exception, request);
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
  public ResponseEntity<ErrorResponse> handleBacklogNotRespondingException(final BacklogNotRespondingException exception,
                                                                           HttpServletRequest request) {
    final ErrorResponse errorResponse = ErrorResponse.builder()
        .status(HttpStatus.UNPROCESSABLE_ENTITY)
        .message(exception.getMessage())
        .error("unprocessable_entity")
        .build();

    request.setAttribute(EXCEPTION_ATTRIBUTE, exception);
    log.error(exception.getMessage(), exception);
    return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
  }

  @ExceptionHandler(ForecastWorkersInvalidException.class)
  public ResponseEntity<ErrorResponse> handleForecastWorkersInvalidException(
      ForecastWorkersInvalidException exception,
      HttpServletRequest request) {

    log.error(exception.getMessage(), exception);

    final ErrorResponse errorResponse = ErrorResponse.builder()
        .status(HttpStatus.BAD_REQUEST)
        .message(exception.getMessage())
        .code(exception.getCode())
        .error(BAD_REQUEST_ERROR)
        .build();

    request.setAttribute(EXCEPTION_ATTRIBUTE, exception);
    return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
  }

  @ExceptionHandler(InvalidSheetVersionException.class)
  public ResponseEntity<ErrorResponse> handleInvalidSheetVersionException(
      final InvalidSheetVersionException exception,
      final HttpServletRequest request) {

    log.error(exception.getMessage(), exception);
    return getBadRequestResponseEntity(exception, request);
  }

  private ResponseEntity<ErrorResponse> getBadRequestResponseEntity(
      Exception exception,
      HttpServletRequest request) {
    ErrorResponse errorResponse = ErrorResponse.builder()
        .status(HttpStatus.BAD_REQUEST)
        .message(exception.getMessage())
        .error(BAD_REQUEST_ERROR)
        .build();

    request.setAttribute(EXCEPTION_ATTRIBUTE, exception);
    return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
  }
}
