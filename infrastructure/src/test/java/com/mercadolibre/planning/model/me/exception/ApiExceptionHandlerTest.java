package com.mercadolibre.planning.model.me.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
