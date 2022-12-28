package com.mercadolibre.planning.model.me.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.mercadolibre.planning.model.me.clients.rest.planningmodel.exception.ForecastNotFoundException;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;

class ApiExceptionHandlerTest {

    private static final String EXCEPTION_ATTRIBUTE = "application.exception";
    private ApiExceptionHandler apiExceptionHandler;
    private HttpServletRequest request;
    private ResponseEntity<ErrorResponse> response;

    @BeforeEach
    void setUp() {
        apiExceptionHandler = new ApiExceptionHandler();
        request = mock(HttpServletRequest.class);
    }

    @Test
    @DisplayName("Handle ForecastParsingException")
    void handleForecastParsingException() {
        // GIVEN
        final ForecastParsingException exception = new ForecastParsingException("test error");
        final ErrorResponse expectedResponse = ErrorResponse.builder()
                .error("bad_request")
                .message("test error")
                .status(HttpStatus.BAD_REQUEST).build();
        // WHEN
        response = apiExceptionHandler.handleException(exception, request);
        // THEN
        thenThrow(exception, expectedResponse);
    }

    @Test
    @DisplayName("Handle UnmatchedException")
    void handleUnmatchedWarehouseException() {
        // GIVEN
        final UnmatchedWarehouseException exception =
                new UnmatchedWarehouseException("ARTW01", "ARTW02");
        final ErrorResponse expectedResponse = ErrorResponse.builder()
                .error("bad_request")
                .message("Warehouse id ARTW01 is different from warehouse id ARTW02 from file")
                .status(HttpStatus.BAD_REQUEST).build();

        // WHEN
        response = apiExceptionHandler.handleUnmatchedWarehouseException(exception, request);

        // THEN
        thenThrow(exception, expectedResponse);
    }

    @Test
    @DisplayName("Handle MissingServletRequestParameterException")
    void handleMissingServletRequestParameterException() {
        // GIVEN
        final MissingServletRequestParameterException exception =
                new MissingServletRequestParameterException("warehouseId", "string");
        final ErrorResponse expectedResponse = ErrorResponse.builder()
                .error("missing_parameter")
                .message(exception.getMessage())
                .status(HttpStatus.BAD_REQUEST).build();

        // WHEN
        response = apiExceptionHandler.handleMissingParameterException(exception, request);

        // THEN
        thenThrow(exception, expectedResponse);
    }

    @Test
    @DisplayName("Handle ForecastNotFoundException")
    void handleForecastNotFoundException() {
      // GIVEN
      final ForecastNotFoundException exception = new ForecastNotFoundException();
      final ErrorResponse expectedResponse = ErrorResponse.builder()
          .error("forecast_not_found")
          .message(exception.getMessage())
          .status(HttpStatus.NOT_FOUND).build();

      // WHEN
      response = apiExceptionHandler.handleForecastNotFoundException(exception, request);

      // THEN
      thenThrow(exception, expectedResponse);
    }

  @Test
  @DisplayName("Handle IllegalArgumentException")
  void handleIllegalArgumentExceptionException() {
    // GIVEN
    final IllegalArgumentException exception = new IllegalArgumentException("");
    final ErrorResponse expectedResponse = ErrorResponse.builder()
        .error("bad_request")
        .message("")
        .status(HttpStatus.BAD_REQUEST).build();

    // WHEN
    response = apiExceptionHandler.handleIllegalArgumentException(exception, request);

    // THEN
    thenThrow(exception, expectedResponse);
  }


  @Test
  @DisplayName("Handle NoPlannedDataForecastNotFoundException")
  void handleNoPlannedDataForecastNotFoundException() {
    // GIVEN
    final NoPlannedDataException exception =
        new NoPlannedDataException();

    final ErrorResponse expectedResponse = ErrorResponse.builder()
        .error("no_planned_data_forecast_not_found")
        .message(exception.getMessage())
        .status(HttpStatus.NOT_FOUND).build();

    // WHEN
    response = apiExceptionHandler.handleNoPlannedDataException(exception, request);

    // THEN
    thenThrow(exception, expectedResponse);
  }

    @Test
    @DisplayName("Handle Exception")
    void handleGenericException() {
        // GIVEN
        final Exception exception = new Exception("Unknown error");
        final ErrorResponse expectedResponse = ErrorResponse.builder()
                .error("unknown_error")
                .message(exception.getMessage())
                .status(HttpStatus.INTERNAL_SERVER_ERROR).build();

        // WHEN
        response = apiExceptionHandler.handleGenericException(exception, request);

        // THEN
        thenThrow(exception, expectedResponse);
    }

    @Test
    @DisplayName("Handle NullValueAtCellException")
    void handleNullValueAtCellException() {
        // GIVEN
        final NullValueAtCellException exception = new NullValueAtCellException("C2");
        final ErrorResponse expectedResponse = ErrorResponse.builder()
                .error("bad_request")
                .message("El buffer (C2) no puede estar vacío, y debe ser un número válido")
                .status(HttpStatus.BAD_REQUEST)
                .build();

        // WHEN
        response = apiExceptionHandler.handleException(exception, request);

        // THEN
        thenThrow(exception, expectedResponse);
    }

    @Test
    @DisplayName("Handle ForecastWorkersInvalidException")
    void handleForecastWorkersInvalidException() {
        // GIVEN
        final ForecastWorkersInvalidException exception = new ForecastWorkersInvalidException("test error", "01");
        final ErrorResponse expectedResponse = ErrorResponse.builder()
            .error("bad_request")
            .message("test error")
            .code("01")
            .status(HttpStatus.BAD_REQUEST).build();
        // WHEN
        response = apiExceptionHandler.handleForecastWorkersInvalidException(exception, request);
        // THEN
        thenThrow(exception, expectedResponse);
    }

    private void thenThrow(Exception exception, ErrorResponse expectedResponse) {
        verify(request).setAttribute(EXCEPTION_ATTRIBUTE, exception);

        assertThat(response).isNotNull();

        final ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getError()).isEqualTo(expectedResponse.getError());
        assertThat(errorResponse.getStatus()).isEqualTo(expectedResponse.getStatus());
        assertThat(errorResponse.getMessage()).startsWith(expectedResponse.getMessage());
    }

}
