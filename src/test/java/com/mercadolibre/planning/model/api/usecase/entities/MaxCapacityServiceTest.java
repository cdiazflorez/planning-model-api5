package com.mercadolibre.planning.model.api.usecase.entities;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.GLOBAL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.MAX_CAPACITY;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.THROUGHPUT;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionView;
import com.mercadolibre.planning.model.api.domain.usecase.entities.maxcapacity.get.MaxCapacityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.maxcapacity.get.MaxCapacityService;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.exception.BadSimulationRequestException;
import com.mercadolibre.planning.model.api.usecase.ProcessingDistributionViewImpl;
import com.mercadolibre.planning.model.api.web.controller.projection.request.QuantityByDate;
import com.mercadolibre.planning.model.api.web.controller.simulation.Simulation;
import com.mercadolibre.planning.model.api.web.controller.simulation.SimulationEntity;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MaxCapacityServiceTest {

  private static final List<Long> FORECASTS_IDS = List.of(1L, 2L);

  private static final String WAREHOUSE_ID = "ARBA01";

  private static final ZonedDateTime NOW = ZonedDateTime.parse("2022-07-07T12:00:00Z");

  private static final ZonedDateTime TRUNCATED_NOW = NOW.truncatedTo(HOURS);

  private static final ZonedDateTime FROM = TRUNCATED_NOW;

  private static final ZonedDateTime TO = TRUNCATED_NOW.plusHours(8);

  private static final List<ZonedDateTime> OPERATING_HOURS = of(
      TRUNCATED_NOW,
      TRUNCATED_NOW.plusHours(1),
      TRUNCATED_NOW.plusHours(2),
      TRUNCATED_NOW.plusHours(4),
      TRUNCATED_NOW.plusHours(5),
      TRUNCATED_NOW.plusHours(6),
      TRUNCATED_NOW.plusHours(7),
      TRUNCATED_NOW.plusHours(8)
  );


  @InjectMocks
  private MaxCapacityService maxCapacityService;

  @Mock
  private ProcessingDistributionRepository processingDistRepository;

  @Mock
  private GetForecastUseCase getForecastUseCase;

  @Test
  public void maxCapacityWithoutSimulations(){
    //GIVEN
    mockGetForecastsIds();
    mockMaxCaps();

    //WHEN
    Map<ZonedDateTime, Integer> result = maxCapacityService.getMaxCapacity(mockInputWithoutSimulations());

    //THEN
    Assertions.assertNotNull(result);
    Assertions.assertEquals(result.get(TRUNCATED_NOW.plusHours(3)), 800);

  }

  @Test
  public void maxCapacityWithSimulations(){
    //GIVEN
    mockGetForecastsIds();
    mockMaxCaps();

    //WHEN
    Map<ZonedDateTime, Integer> result = maxCapacityService.getMaxCapacity(mockInputWithSimulations());

    //THEN
    Assertions.assertNotNull(result);
    Assertions.assertEquals(result.get(TRUNCATED_NOW), 130);

  }

  @Test
  public void maxCapacityWithBadRequestSimulations(){
    //GIVEN
    mockGetForecastsIds();
    mockMaxCaps();

    //WHEN
    Exception exception = assertThrows(BadSimulationRequestException.class, () -> {
      maxCapacityService.getMaxCapacity(mockInputWithBadRequestSimulations());
    });

    String expectedMessage = "Duplicate SimulationEntity with name THROUGHPUT";
    String actualMessage = exception.getMessage();

    assertTrue(actualMessage.contains(expectedMessage));

  }

  private void mockMaxCaps() {
    final List<ProcessingDistributionView> caps = OPERATING_HOURS.stream()
        .map(ZonedDateTime::toInstant)
        .map(date -> ProcessingDistributionViewImpl.builder()
            .date(Date.from(date))
            .quantity(800L)
            .build()
        ).collect(Collectors.toList());

    when(processingDistRepository.findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
        Set.of(MAX_CAPACITY.name()),
        of(GLOBAL.toJson()),
        FROM,
        TO,
        FORECASTS_IDS)
    ).thenReturn(caps);
  }

  private void mockGetForecastsIds() {
    when(getForecastUseCase.execute(GetForecastInput.builder()
        .warehouseId(WAREHOUSE_ID)
        .workflow(FBM_WMS_OUTBOUND)
        .dateFrom(FROM)
        .dateTo(TO)
        .build())
    ).thenReturn(FORECASTS_IDS);
  }

  private MaxCapacityInput mockInputWithoutSimulations(){
    return new  MaxCapacityInput(WAREHOUSE_ID,FBM_WMS_OUTBOUND,FROM,TO, Collections.emptyList());
  }

  private MaxCapacityInput mockInputWithSimulations(){
    return new  MaxCapacityInput(WAREHOUSE_ID,FBM_WMS_OUTBOUND,FROM,TO, List.of(new Simulation(
        GLOBAL,
        List.of(new SimulationEntity(
            THROUGHPUT,
            List.of(
                new QuantityByDate(TRUNCATED_NOW,
                    130))
        ))
    )));
  }

  private MaxCapacityInput mockInputWithBadRequestSimulations(){
    return new  MaxCapacityInput(WAREHOUSE_ID,FBM_WMS_OUTBOUND,FROM,TO, List.of(new Simulation(
        GLOBAL,
        List.of(new SimulationEntity(
            THROUGHPUT,
            List.of(
                new QuantityByDate(TRUNCATED_NOW,
                    130))
        ),
            new SimulationEntity(
                THROUGHPUT,
                List.of(
                    new QuantityByDate(TRUNCATED_NOW,
                        130))
            ))
    )));
  }

}
