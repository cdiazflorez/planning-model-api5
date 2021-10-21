package com.mercadolibre.planning.model.api.exception;

import com.mercadolibre.fbm.wms.outbound.commons.web.response.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;

import java.util.Set;
import org.springframework.validation.BindException;

import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ApiExceptionHandlerTest {

    private static final String EXCEPTION_ATTRIBUTE = "application.exception";
    private ApiExceptionHandler apiExceptionHandler;
    private HttpServletRequest request;

    @BeforeEach
    public void setUp() {
        apiExceptionHandler = new ApiExceptionHandler();
        request = mock(HttpServletRequest.class);
    }

    @Test
    @DisplayName("Handle BindException")
    void handleBindException() {
        // GIVEN
        final BindException exception = new BindException("warehouseId", "string");
        final ErrorResponse expectedResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                "missing_parameter"
        );

        // WHEN
        final ResponseEntity<ErrorResponse> response = apiExceptionHandler.handleBindException(
                exception, request);

        // THEN
        assertErrorResponse(expectedResponse, response);
    }

    @Test
    public void handleInvalidEntityTypeException() {
        // GIVEN
        final InvalidEntityTypeException exception = new InvalidEntityTypeException("invalid");
        final ErrorResponse expectedResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Value invalid is invalid, instead it should be one of"
                        + " [HEADCOUNT, PRODUCTIVITY, THROUGHPUT, REMAINING_PROCESSING,"
                        + " PERFORMED_PROCESSING, BACKLOG_LOWER_LIMIT, BACKLOG_UPPER_LIMIT]",
                "invalid_entity_type");

        // WHEN
        final ResponseEntity<ErrorResponse> response =
                apiExceptionHandler.handleInvalidEntityTypeException(exception, request);

        // THEN
        verify(request).setAttribute(EXCEPTION_ATTRIBUTE, exception);
        assertErrorResponse(expectedResponse, response);
    }

    @Test
    public void handleProjectionTypeException() {
        // GIVEN
        final InvalidProjectionTypeException exception =
                new InvalidProjectionTypeException("invalid");

        final ErrorResponse expectedResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Value invalid is invalid, instead it should be one of"
                        + " [BACKLOG, CPT, DEFERRAL]", "invalid_projection_type");

        // WHEN
        final ResponseEntity<ErrorResponse> response =
                apiExceptionHandler.handleInvalidProjectionTypeException(exception, request);

        // THEN
        verify(request).setAttribute(EXCEPTION_ATTRIBUTE, exception);
        assertErrorResponse(expectedResponse, response);
    }

    @Test
    public void handleProjectionTypeNotSupportedException() {
        // GIVEN
        final ProjectionTypeNotSupportedException exception =
                new ProjectionTypeNotSupportedException(null);

        final ErrorResponse expectedResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Projection type null is not supported", "projection_type_not_supported");

        // WHEN
        final ResponseEntity<ErrorResponse> response =
                apiExceptionHandler.handleProjectionTypeNotSupportedException(exception, request);

        // THEN
        verify(request).setAttribute(EXCEPTION_ATTRIBUTE, exception);
        assertErrorResponse(expectedResponse, response);
    }

    @Test
    public void handleEntityTypeNotSupportedException() {
        // GIVEN
        final EntityTypeNotSupportedException exception = new EntityTypeNotSupportedException(null);
        final ErrorResponse expectedResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Entity type null is not supported", "entity_type_not_supported");

        // WHEN
        final ResponseEntity<ErrorResponse> response =
                apiExceptionHandler.handleEntityTypeNotSupportedException(exception, request);

        // THEN
        verify(request).setAttribute(EXCEPTION_ATTRIBUTE, exception);
        assertErrorResponse(expectedResponse, response);
    }

    @Test
    public void handleEntityNotFoundException() {
        // GIVEN
        final EntityNotFoundException exception = new EntityNotFoundException(
                "expedition_processing_time", "1");
        final ErrorResponse expectedResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND,
                "Entity expedition_processing_time with id 1 was not found",
                "entity_not_found");

        // WHEN
        final ResponseEntity<ErrorResponse> response =
                apiExceptionHandler.handleEntityNotFoundException(exception, request);

        // THEN
        verify(request).setAttribute(EXCEPTION_ATTRIBUTE, exception);
        assertErrorResponse(expectedResponse, response);
    }

    @Test
    public void handleEntityAlreadyExistsException() {
        // GIVEN
        final EntityAlreadyExistsException exception = new EntityAlreadyExistsException(
                "configuration", "ARBA01-expedition_processing_time");

        final ErrorResponse expectedResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Entity configuration with id ARBA01-expedition_processing_time already exists",
                "entity_already_exists");

        // WHEN
        final ResponseEntity<ErrorResponse> response =
                apiExceptionHandler.handleEntityAlreadyExistsException(exception, request);

        // THEN
        verify(request).setAttribute(EXCEPTION_ATTRIBUTE, exception);
        assertErrorResponse(expectedResponse, response);
    }

    @Test
    public void handleForecastNotFoundException() {
        // GIVEN
        final ForecastNotFoundException exception = new ForecastNotFoundException(
                FBM_WMS_OUTBOUND.toJson(),
                WAREHOUSE_ID,
                Set.of("3-2021")
        );
        final ErrorResponse expectedResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND,
                "Forecast not present for "
                        + "workflow:fbm-wms-outbound, warehouse_id:ARBA01 and weeks:[3-2021]",
                "forecast_not_found"
        );

        // WHEN
        final ResponseEntity<ErrorResponse> response =
                apiExceptionHandler.handleForecastNotFoundException(exception, request);

        // THEN
        verify(request).setAttribute(EXCEPTION_ATTRIBUTE, exception);
        assertErrorResponse(expectedResponse, response);
    }

    @Test
    @DisplayName("Handle Exception")
    public void handleGenericException() {
        // GIVEN
        final Exception exception = new Exception("Unknown error");
        final ErrorResponse expectedResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getMessage(),
                "unknown_error");

        // WHEN
        final ResponseEntity<ErrorResponse> response = apiExceptionHandler.handleGenericException(
                exception, request);

        // THEN
        verify(request).setAttribute(EXCEPTION_ATTRIBUTE, exception);
        assertErrorResponse(expectedResponse, response);
    }


    private void assertErrorResponse(final ErrorResponse expectedResponse,
                                     final ResponseEntity<ErrorResponse> response) {

        assertThat(response).isNotNull();

        final ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getError()).isEqualTo(expectedResponse.getError());
        assertThat(errorResponse.getStatus()).isEqualTo(expectedResponse.getStatus());
        assertThat(errorResponse.getMessage()).startsWith(expectedResponse.getMessage());
    }
}
