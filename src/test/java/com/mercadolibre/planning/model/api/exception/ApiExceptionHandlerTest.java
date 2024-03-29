package com.mercadolibre.planning.model.api.exception;

import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.SHIFTS_PARAMETERS;
import static com.mercadolibre.planning.model.api.domain.usecase.inputcatalog.inputdomain.InputOptionFilter.INCLUDE_DAY_NAME;
import static com.mercadolibre.planning.model.api.domain.usecase.inputcatalog.inputdomain.InputOptionFilter.INCLUDE_SHIFT_GROUP;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

import com.mercadolibre.fbm.wms.outbound.commons.web.response.ErrorResponse;
import com.mercadolibre.planning.model.api.domain.usecase.inputcatalog.inputdomain.InputOptionFilter;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import org.assertj.core.util.VisibleForTesting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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
  @VisibleForTesting
  @DisplayName("Handle BindException")
  void handleBindException() {
    // GIVEN
    final BindException exception = new BindException("warehouseId", "string");
    final ErrorResponse expectedResponse = new ErrorResponse(
        BAD_REQUEST,
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
        BAD_REQUEST,
        "Value invalid is invalid, instead it should be one of"
            + " [HEADCOUNT, HEADCOUNT_SYSTEMIC, HEADCOUNT_NON_SYSTEMIC, PRODUCTIVITY, THROUGHPUT, REMAINING_PROCESSING,"
            + " PERFORMED_PROCESSING, BACKLOG_LOWER_LIMIT, BACKLOG_UPPER_LIMIT,"
            + " BACKLOG_LOWER_LIMIT_SHIPPING, BACKLOG_UPPER_LIMIT_SHIPPING,"
            + " MAX_CAPACITY]",
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
        BAD_REQUEST,
        "Value invalid is invalid, instead it should be one of"
            + " [BACKLOG, CPT, DEFERRAL, COMMAND_CENTER_DEFERRAL, COMMAND_CENTER_SLA]", "invalid_projection_type");

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
        BAD_REQUEST,
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
        BAD_REQUEST,
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
        BAD_REQUEST,
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
  public void handleInvalidForecastException() {
    // GIVEN
    final InvalidForecastException exception = new InvalidForecastException(
        WAREHOUSE_ID,
        FBM_WMS_OUTBOUND.name()
    );

    final ErrorResponse expectedResponse = new ErrorResponse(
        HttpStatus.CONFLICT,
        "The currently loaded forecast is invalid or has missing values, "
            + "warehouse_id:ARBA01, workflow:FBM_WMS_OUTBOUND",
        "invalid_forecast"
    );

    // WHEN
    final ResponseEntity<ErrorResponse> response =
        apiExceptionHandler.handleInvalidForecastException(exception, request);

    // THEN
    verify(request).setAttribute(EXCEPTION_ATTRIBUTE, exception);
    assertErrorResponse(expectedResponse, response);
  }

  @Test
  public void handleInvalidDomainFilterException() {
    //GIVEN
    final InputOptionFilter[] inputOptionFilters = {INCLUDE_DAY_NAME, INCLUDE_SHIFT_GROUP};

    final InvalidInputFilterException exception = new InvalidInputFilterException(SHIFTS_PARAMETERS, inputOptionFilters);

    final ErrorResponse expectedResponse = new ErrorResponse(
        BAD_REQUEST,
        String.format("Input %s only can use %s parameters", SHIFTS_PARAMETERS, Arrays.toString(inputOptionFilters)),
        "invalid_domain_filter"
    );

    //WHEN
    final ResponseEntity<ErrorResponse> response = apiExceptionHandler.handleInvalidDomainFilter(exception, request);

    //THEN
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

  @Test
  @DisplayName("Handle MissingServletRequestParameterException")
  void handleMissingServletRequestParameterException() {
    // GIVEN
    final MissingServletRequestParameterException exception =
        new MissingServletRequestParameterException("warehouseId", "string");
    final ErrorResponse expectedResponse = new ErrorResponse(
        BAD_REQUEST,
        exception.getMessage(),
        "missing_parameter"
    );

    // WHEN
    final ResponseEntity<ErrorResponse> response = apiExceptionHandler.handleMissingParameterException(exception, request);

    // THEN
    assertErrorResponse(expectedResponse, response);
  }

  @Test
  @DisplayName("Handle InvalidDateRangeException")
  void handleInvalidDateRangeException() {
    //GIVEN
    final Instant now = Instant.now();
    final InvalidDateRangeException exception = new InvalidDateRangeException(
        now,
        now.plus(1, HOURS),
        now,
        now.plus(1, HOURS));

    final ErrorResponse expectedResponse = new ErrorResponse(
        BAD_REQUEST,
        exception.getMessage(),
        "invalid_date_range"
    );

    // WHEN
    final ResponseEntity<ErrorResponse> response = apiExceptionHandler.handleInvalidDateRangeException(exception, request);

    // THEN
    assertErrorResponse(expectedResponse, response);
  }

  @Test
  @DisplayName("Handle DateRangeException")
  void dateRangeException() {
    //GIVEN
    final Instant now = Instant.now();
    final DateRangeException exception = new DateRangeException(now.plus(1, HOURS), now);

    final ErrorResponse expectedResponse = new ErrorResponse(
        BAD_REQUEST,
        exception.getMessage(),
        "invalid_date_range"
    );

    // WHEN
    final ResponseEntity<ErrorResponse> response = apiExceptionHandler.handleInvalidDateRangeException(exception, request);

    // THEN
    assertErrorResponse(expectedResponse, response);
  }


  @Test
  @DisplayName("Handle InvalidDateToSaveDeviationException")
  void handleInvalidDateToSaveDeviationExceptionTest() {
    //GIVEN
    final Instant now = Instant.now();
    final InvalidDateToSaveDeviationException exception = new InvalidDateToSaveDeviationException(
        now.minus(2, HOURS),
        now.plus(4, HOURS),
        now);

    final ErrorResponse expectedResponse = new ErrorResponse(
        BAD_REQUEST,
        exception.getMessage(),
        "invalid_date_range_to_save_deviation"
    );

    // WHEN
    final ResponseEntity<ErrorResponse> response = apiExceptionHandler.handleInvalidDateToSaveDeviationException(exception, request);

    // THEN
    assertErrorResponse(expectedResponse, response);
  }

  @Test
  @DisplayName("Handle InvalidArgumentException")
  void handleInvalidArgumentExceptionTest() {
    //GIVEN
    final var exception = new InvalidArgumentException("Invalid argument");

    final ErrorResponse expectedResponse = new ErrorResponse(
        BAD_REQUEST,
        exception.getMessage(),
        "invalid_arguments"
    );

    // WHEN
    final ResponseEntity<ErrorResponse> response = apiExceptionHandler.handleInvalidArgumentException(exception, request);

    // THEN
    assertErrorResponse(expectedResponse, response);
  }

  @Test
  @DisplayName("Handle InvalidDateToSaveDeviationException")
  void handleInvalidDateRangeDeviationsExceptionTest() {
    //GIVEN
    final Instant now = Instant.now();
    final InvalidDateRangeDeviationsException exception = new InvalidDateRangeDeviationsException(
        now.plus(2, HOURS),
        now);

    final ErrorResponse expectedResponse = new ErrorResponse(
        BAD_REQUEST,
        exception.getMessage(),
        "invalid_date_range_deviations"
    );

    // WHEN
    final ResponseEntity<ErrorResponse> response = apiExceptionHandler.handleInvalidDateRangeDeviationsException(exception, request);

    // THEN
    assertErrorResponse(expectedResponse, response);


  }

  @Test
  @DisplayName("Handle UnexpiredDeviationPresentException")
  void handlePercentageDeviationUnexpiredExceptionTest() {
    //GIVEN
    final UnexpiredDeviationPresentException exception = new UnexpiredDeviationPresentException(
        "There is a deviation percentage in effect."
    );

    final ErrorResponse expectedResponse = new ErrorResponse(
        CONFLICT,
        exception.getMessage(),
        "percentage_deviation_Unexpired"
    );

    // WHEN
    final ResponseEntity<ErrorResponse> response = apiExceptionHandler.handlePercentageDeviationUnexpiredException(exception, request);

    // THEN
    assertErrorResponse(expectedResponse, response);
  }

  @Test
  @DisplayName("Handle PercentageDeviationUnexpiredException")
  void handleDeviationsToSaveNotFoundExceptionTest() {
    //GIVEN
    final DeviationsToSaveNotFoundException exception = new DeviationsToSaveNotFoundException();

    final ErrorResponse expectedResponse = new ErrorResponse(
        BAD_REQUEST,
        exception.getMessage(),
        "deviations_to_save_not_found"
    );

    // WHEN
    final ResponseEntity<ErrorResponse> response = apiExceptionHandler.handleDeviationsToSaveNotFoundException(exception, request);

    // THEN
    assertErrorResponse(expectedResponse, response);
  }

  @Test
  void handleMethodArgumentTypeMismatchExceptionTest() {
    //GIVEN
    final MethodArgumentTypeMismatchException exception =
        new MethodArgumentTypeMismatchException(null, null, anyString(), any(), new Exception());

    final ErrorResponse expectedResponse = new ErrorResponse(
        BAD_REQUEST,
        exception.getMessage(),
        "method_argument_type_mismatch_exception"
    );

    // WHEN
    final ResponseEntity<ErrorResponse> response = apiExceptionHandler.handleMethodArgumentTypeMismatchException(exception, request);

    // THEN
    assertErrorResponse(expectedResponse, response);
  }

  @Test
  void handleConstraintViolationExceptionTest() {
    //GIVEN
    final ConstraintViolationException exception = new ConstraintViolationException("test", Set.of());

    final ErrorResponse expectedResponse = new ErrorResponse(
        BAD_REQUEST,
        exception.getMessage(),
        "constraint_violation_exception"
    );

    // WHEN
    final ResponseEntity<ErrorResponse> response = apiExceptionHandler.handleConstraintViolationException(exception, request);

    // THEN
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
