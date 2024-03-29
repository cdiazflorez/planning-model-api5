package com.mercadolibre.planning.model.api.usecase.entities;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.GLOBAL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockCurrentProdEntity;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockGetProductivityEntityInput;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.FORECAST;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.SIMULATION;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.HeadcountProductivityRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.HeadcountProductivityView;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.ProductivityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.usecase.HeadcountProductivityViewImpl;
import com.mercadolibre.planning.model.api.web.controller.projection.request.QuantityByDate;
import com.mercadolibre.planning.model.api.web.controller.projection.request.Source;
import com.mercadolibre.planning.model.api.web.controller.simulation.Simulation;
import com.mercadolibre.planning.model.api.web.controller.simulation.SimulationEntity;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GetProductivityEntityUseCaseTest {

  @Mock
  private HeadcountProductivityRepository productivityRepository;

  @Mock
  private CurrentProcessingDistributionRepository processingDistributionRepository;

  @Mock
  private GetForecastUseCase getForecastUseCase;

  @InjectMocks
  private GetProductivityEntityUseCase getProductivityEntityUseCase;

  @Test
  @DisplayName("Get productivity entity when source is forecast")
  public void testGetProductivityOk() {
    // GIVEN
    final GetProductivityInput input = mockGetProductivityEntityInput(FORECAST, null, List.of(GLOBAL));
    final List<Long> forecastIds = singletonList(1L);

    when(getForecastUseCase.execute(GetForecastInput.builder()
        .workflow(input.getWorkflow())
        .warehouseId(input.getWarehouseId())
        .dateFrom(input.getDateFrom())
        .dateTo(input.getDateTo())
        .viewDate(A_DATE_UTC.toInstant())
        .build())
    ).thenReturn(forecastIds);

    when(productivityRepository.findBy(
        List.of(PICKING.name(), PACKING.name()),
        List.of(GLOBAL.name()),
        input.getDateFrom(),
        input.getDateTo(),
        forecastIds,
        Set.of(1))
    ).thenReturn(productivities());

    // WHEN
    final List<ProductivityOutput> output = getProductivityEntityUseCase.execute(input);

    // THEN
    assertEquals(4, output.size());
    verifyNoInteractions(processingDistributionRepository);
    outputPropertiesEqualTo(output.get(0), PICKING, FORECAST, 80);
    outputPropertiesEqualTo(output.get(1), PICKING, FORECAST, 85);
    outputPropertiesEqualTo(output.get(2), PACKING, FORECAST, 90);
    outputPropertiesEqualTo(output.get(3), PACKING, FORECAST, 92);
  }

  @Test
  @DisplayName("Get productivity entity when source is null and has simulations applied")
  public void testGetProductivityWithUnsavedSimulationOk() {
    // GIVEN
    final GetProductivityInput input = mockGetProductivityEntityInput(
        null,
        List.of(new Simulation(
            PICKING,
            List.of(new SimulationEntity(
                PRODUCTIVITY,
                List.of(new QuantityByDate(A_DATE_UTC, 100D, null),
                    new QuantityByDate(A_DATE_UTC.plusHours(1), 101D, null)))))),
        null);

    final List<Long> forecastIds = List.of(1L);

    when(getForecastUseCase.execute(GetForecastInput.builder()
        .workflow(input.getWorkflow())
        .warehouseId(input.getWarehouseId())
        .dateFrom(input.getDateFrom())
        .dateTo(input.getDateTo())
        .viewDate(A_DATE_UTC.toInstant())
        .build())
    ).thenReturn(forecastIds);

    when(processingDistributionRepository
        .findSimulationByWarehouseIdWorkflowTypeProcessNameAndDateInRangeAtViewDate(
            WAREHOUSE_ID,
            FBM_WMS_OUTBOUND.name(),
            Set.of(),
            Set.of(PICKING.name(), PACKING.name()),
            Set.of(PRODUCTIVITY.name()),
            input.getDateFrom(),
            input.getDateTo(),
            A_DATE_UTC.toInstant()
        )
    ).thenReturn(List.of(
        mockCurrentProdEntity(A_DATE_UTC, 68),
        mockCurrentProdEntity(A_DATE_UTC.plusHours(1), 30)
    ));

    when(productivityRepository.findBy(
        List.of(PICKING.name(), PACKING.name()),
        List.of(),
        input.getDateFrom(),
        input.getDateTo(),
        forecastIds,
        Set.of(1))
    ).thenReturn(productivities());

    // WHEN
    final List<ProductivityOutput> output = getProductivityEntityUseCase.execute(input);

    // THEN
    assertEquals(6, output.size());

    outputPropertiesEqualTo(output.get(0), PICKING, FORECAST, 80);
    outputPropertiesEqualTo(output.get(1), PICKING, FORECAST, 85);
    outputPropertiesEqualTo(output.get(2), PACKING, FORECAST, 90);
    outputPropertiesEqualTo(output.get(3), PACKING, FORECAST, 92);
    outputPropertiesEqualTo(output.get(4), PICKING, SIMULATION, 100);
    outputPropertiesEqualTo(output.get(5), PICKING, SIMULATION, 101);
  }

  @Test
  @DisplayName("Get productivity entity when source is simulation")
  public void testGetProductivityFromSourceSimulation() {
    // GIVEN
    final GetProductivityInput input = mockGetProductivityEntityInput(SIMULATION, null, emptyList());
    final CurrentProcessingDistribution currentProd = mockCurrentProdEntity(A_DATE_UTC, 68L);
    final List<Long> forecastIds = List.of(1L);

    // WHEN
    when(getForecastUseCase.execute(GetForecastInput.builder()
        .workflow(input.getWorkflow())
        .warehouseId(input.getWarehouseId())
        .dateFrom(input.getDateFrom())
        .dateTo(input.getDateTo())
        .viewDate(A_DATE_UTC.toInstant())
        .build())
    ).thenReturn(forecastIds);

    when(productivityRepository.findBy(
        List.of(PICKING.name(), PACKING.name()),
        List.of(),
        input.getDateFrom(),
        input.getDateTo(),
        forecastIds,
        Set.of(1))
    ).thenReturn(productivities());

    when(processingDistributionRepository
        .findSimulationByWarehouseIdWorkflowTypeProcessNameAndDateInRangeAtViewDate(
            currentProd.getLogisticCenterId(),
            FBM_WMS_OUTBOUND.name(),
            Set.of(),
            Set.of(PICKING.name(), PACKING.name()),
            Set.of(PRODUCTIVITY.name()),
            input.getDateFrom(),
            input.getDateTo(),
            A_DATE_UTC.toInstant()
        )
    ).thenReturn(currentProductivities(Set.of(GLOBAL)));

    final List<ProductivityOutput> output = getProductivityEntityUseCase.execute(input);

    // THEN
    assertThat(output).isNotEmpty();
    assertEquals(5, output.size());
    outputPropertiesEqualTo(output.get(0), PICKING, FORECAST, 80);
    outputPropertiesEqualTo(output.get(1), PICKING, FORECAST, 85);
    outputPropertiesEqualTo(output.get(2), PACKING, FORECAST, 90);
    outputPropertiesEqualTo(output.get(3), PACKING, FORECAST, 92);
    outputPropertiesEqualTo(output.get(4), PICKING, SIMULATION, 68);
  }

  @Test
  @DisplayName("Get productivity entity when source is simulation and contains a process path other than global")
  public void testGetProductivityFromSourceSimulationAndProcessPath() {
    // GIVEN
    final GetProductivityInput input = mockGetProductivityEntityInput(SIMULATION, null, List.of(TOT_MONO));
    final List<Long> forecastIds = List.of(1L);

    // WHEN
    when(getForecastUseCase.execute(GetForecastInput.builder()
        .workflow(input.getWorkflow())
        .warehouseId(input.getWarehouseId())
        .dateFrom(input.getDateFrom())
        .dateTo(input.getDateTo())
        .viewDate(A_DATE_UTC.toInstant())
        .build())
    ).thenReturn(forecastIds);

    when(productivityRepository.findBy(
        List.of(PICKING.name(), PACKING.name()),
        List.of(TOT_MONO.name()),
        input.getDateFrom(),
        input.getDateTo(),
        forecastIds,
        Set.of(1))
    ).thenReturn(productivities());

    final List<ProductivityOutput> output = getProductivityEntityUseCase.execute(input);

    // THEN
    assertThat(output).isNotEmpty();
    assertEquals(4, output.size());
    outputPropertiesEqualTo(output.get(0), PICKING, FORECAST, 80);
    outputPropertiesEqualTo(output.get(1), PICKING, FORECAST, 85);
    outputPropertiesEqualTo(output.get(2), PACKING, FORECAST, 90);
    outputPropertiesEqualTo(output.get(3), PACKING, FORECAST, 92);
  }

  @Test
  @DisplayName("Get productivity entity when source is simulation and contains a process path other than global  and global")
  public void testGetProductivityFromSourceSimulationAndProcessPathWithGlobal() {
    // GIVEN
    final GetProductivityInput input = mockGetProductivityEntityInput(SIMULATION, null, List.of(GLOBAL, TOT_MONO));
    final List<Long> forecastIds = List.of(1L);

    // WHEN
    when(getForecastUseCase.execute(GetForecastInput.builder()
        .workflow(input.getWorkflow())
        .warehouseId(input.getWarehouseId())
        .dateFrom(input.getDateFrom())
        .dateTo(input.getDateTo())
        .viewDate(A_DATE_UTC.toInstant())
        .build())
    ).thenReturn(forecastIds);

    when(productivityRepository.findBy(
        List.of(PICKING.name(), PACKING.name()),
        List.of(GLOBAL.name(), TOT_MONO.name()),
        input.getDateFrom(),
        input.getDateTo(),
        forecastIds,
        Set.of(1))
    ).thenReturn(productivities());

    when(processingDistributionRepository
        .findSimulationByWarehouseIdWorkflowTypeProcessNameAndDateInRangeAtViewDate(
            input.getWarehouseId(),
            input.getWorkflow().name(),
            input.getProcessPaths().stream().map(ProcessPath::name).collect(toSet()),
            Set.of(PICKING.name(), PACKING.name()),
            Set.of(PRODUCTIVITY.name()),
            input.getDateFrom(),
            input.getDateTo(),
            input.viewDate()
        )).thenReturn(currentProductivities(Set.of(GLOBAL, TOT_MONO)));

    final List<ProductivityOutput> output = getProductivityEntityUseCase.execute(input);

    // THEN
    assertThat(output).isNotEmpty();
    assertEquals(6, output.size());
    outputPropertiesEqualTo(output.get(0), PICKING, FORECAST, 80);
    outputPropertiesEqualTo(output.get(1), PICKING, FORECAST, 85);
    outputPropertiesEqualTo(output.get(2), PACKING, FORECAST, 90);
    outputPropertiesEqualTo(output.get(3), PACKING, FORECAST, 92);
    outputPropertiesEqualTo(output.get(4), PICKING, SIMULATION, 68);
    outputPropertiesEqualTo(output.get(5), PICKING, SIMULATION, 68);
    assertTrue(output.stream()
        .filter(productivityOutput -> productivityOutput.getSource() == SIMULATION)
        .anyMatch(productivityOutput -> productivityOutput.getProcessPath() == GLOBAL));
    assertTrue(output.stream()
        .filter(productivityOutput -> productivityOutput.getSource() == SIMULATION)
        .anyMatch(productivityOutput -> productivityOutput.getProcessPath() == TOT_MONO));
  }

  private List<HeadcountProductivityView> productivities() {
    return List.of(
        new HeadcountProductivityViewImpl(PICKING,
            80, UNITS_PER_HOUR, Date.from(A_DATE_UTC.toInstant()), 1, GLOBAL),
        new HeadcountProductivityViewImpl(PICKING,
            85, UNITS_PER_HOUR, Date.from(A_DATE_UTC.plusHours(1).toInstant()), 1, GLOBAL),
        new HeadcountProductivityViewImpl(PACKING,
            90, UNITS_PER_HOUR, Date.from(A_DATE_UTC.toInstant()), 1, GLOBAL),
        new HeadcountProductivityViewImpl(PACKING,
            92, UNITS_PER_HOUR, Date.from(A_DATE_UTC.plusHours(1).toInstant()), 1, GLOBAL)
    );
  }

  private List<CurrentProcessingDistribution> currentProductivities(final Set<ProcessPath> processPaths) {
    return processPaths.stream().map(processPath ->
        CurrentProcessingDistribution
            .builder()
            .date(A_DATE_UTC)
            .isActive(true)
            .quantity(68)
            .quantityMetricUnit(UNITS_PER_HOUR)
            .processName(PICKING)
            .processPath(processPath)
            .logisticCenterId(WAREHOUSE_ID)
            .workflow(FBM_WMS_OUTBOUND)
            .build()
    ).toList();
  }

  private void outputPropertiesEqualTo(final ProductivityOutput productivityOutput,
                                       final ProcessName processName,
                                       final Source source,
                                       final int quantity) {

    assertEquals(processName, productivityOutput.getProcessName());
    assertEquals(source, productivityOutput.getSource());
    assertEquals(quantity, productivityOutput.getValue());
    assertEquals(UNITS_PER_HOUR, productivityOutput.getMetricUnit());
    assertEquals(FBM_WMS_OUTBOUND, productivityOutput.getWorkflow());
    assertThat(productivityOutput.getAbilityLevel()).isIn(1, 2);
  }
}
