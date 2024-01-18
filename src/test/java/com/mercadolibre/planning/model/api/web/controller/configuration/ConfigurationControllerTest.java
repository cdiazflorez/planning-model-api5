package com.mercadolibre.planning.model.api.web.controller.configuration;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.CONFIG_KEY;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static java.lang.String.format;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.service.sla.OutboundSlaPropertiesService;
import com.mercadolibre.planning.model.api.domain.service.sla.SlaProperties;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.create.ConfigurationInput;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.create.CreateConfigurationUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.get.GetConfigurationByKeyUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.update.UpdateConfigurationUseCase;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = ConfigurationController.class)
class ConfigurationControllerTest {

  private static final String LOGISTIC_CENTER_ID = "ARBA01";

  private static final String KEY = "expedition_processing_time";

  private static final String URL = "/planning/model/configuration";

  private static final String UPDATE_URL = "/planning/model/configuration/%s/%s";

  private static final String CYCLE_TIME_PATH = "/logistic_center/{lc}/cycle_time/search";

  private static final String USER_ID_FIELD = "user_id";

  private static final Instant DATE_FROM = A_DATE_UTC.toInstant();

  private static final Instant DATE_TO = DATE_FROM.plus(3, ChronoUnit.HOURS);

  @Autowired
  private MockMvc mvc;

  @MockBean
  private GetConfigurationByKeyUseCase getConfiguration;

  @MockBean
  private CreateConfigurationUseCase createConfiguration;

  @MockBean
  private UpdateConfigurationUseCase updateConfiguration;

  @MockBean
  private OutboundSlaPropertiesService outboundSlaPropertiesService;

  public static Stream<Arguments> testCreateAndUpdateArguments() {
    return Stream.of(
        Arguments.of(null, 0),
        Arguments.of("123", 123)
    );
  }

  @ParameterizedTest
  @MethodSource("testCreateAndUpdateArguments")
  void testCreateConfiguration(final String userParam, final long userId) throws Exception {
    // GIVEN
    final ConfigurationInput input = new ConfigurationInput(
        LOGISTIC_CENTER_ID, KEY, 60, MINUTES, userId);

    when(createConfiguration.execute(input)).thenReturn(Configuration.builder()
        .logisticCenterId(LOGISTIC_CENTER_ID)
        .key(CONFIG_KEY)
        .value("60")
        .metricUnit(MINUTES)
        .build());

    // WHEN
    final ResultActions result = mvc.perform(
        post(URL)
            .contentType(APPLICATION_JSON)
            .param(USER_ID_FIELD, userParam)
            .content(getResourceAsString("post_configuration.json"))
    );

    // THEN
    result.andExpect(status().isOk())
        .andExpect(jsonPath("$.value").value(60))
        .andExpect(jsonPath("$.metric_unit").value(MINUTES.toJson()));
  }

  @ParameterizedTest
  @MethodSource("testCreateAndUpdateArguments")
  void testUpdateConfiguration(final String userParam, final long userId) throws Exception {
    // GIVEN
    final ConfigurationInput input = new ConfigurationInput(
        LOGISTIC_CENTER_ID, KEY, 180, MINUTES, userId);

    when(updateConfiguration.execute(input)).thenReturn(Configuration.builder()
        .logisticCenterId(LOGISTIC_CENTER_ID)
        .key(CONFIG_KEY)
        .value("180")
        .metricUnit(MINUTES)
        .build());

    // WHEN
    final ResultActions result = mvc.perform(
        put(format(UPDATE_URL, LOGISTIC_CENTER_ID, KEY))
            .contentType(APPLICATION_JSON)
            .param(USER_ID_FIELD, userParam)
            .content(getResourceAsString("put_configuration.json"))
    );

    // THEN
    result.andExpect(status().isOk())
        .andExpect(jsonPath("$.value").value(180))
        .andExpect(jsonPath("$.metric_unit").value(MINUTES.toJson()));
  }

  @Test
  void testGetOutboundCycleTimeOk() throws Exception {
    // GIVEN
    when(outboundSlaPropertiesService.get(
        new OutboundSlaPropertiesService.Input(
            WAREHOUSE_ID,
            FBM_WMS_OUTBOUND,
            DATE_FROM,
            DATE_TO,
            List.of(DATE_FROM, DATE_FROM.plus(1, ChronoUnit.HOURS), DATE_FROM.plus(2, ChronoUnit.HOURS)),
            "UTC"
        )
    )).thenReturn(
        Map.of(
            A_DATE_UTC.toInstant(), new SlaProperties(10L),
            A_DATE_UTC.plusHours(1).toInstant(), new SlaProperties(20L),
            A_DATE_UTC.plusHours(2).toInstant(), new SlaProperties(30L),
            A_DATE_UTC.plusHours(3).toInstant(), new SlaProperties(40L)
        )
    );

    // WHEN
    final ResultActions result = mvc.perform(
        post(URL + CYCLE_TIME_PATH, "ARBA01")
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("search_cycle_times.json"))
    );

    // THEN
    result.andExpect(status().isOk())
        .andExpect(content().json(getResourceAsString("search_cycle_times_response.json")));
  }
}
