package com.mercadolibre.planning.model.api.usecase;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.DateUtils.ignoreMinutes;
import static com.mercadolibre.planning.model.api.util.ProjectionTestsUtils.A_FIXED_DATE;
import static com.mercadolibre.planning.model.api.util.ProjectionTestsUtils.assertCapacityByDate;
import static com.mercadolibre.planning.model.api.util.ProjectionTestsUtils.getMinCapacity;
import static com.mercadolibre.planning.model.api.util.ProjectionTestsUtils.mockBacklogProjectionInput;
import static com.mercadolibre.planning.model.api.util.ProjectionTestsUtils.mockPlanningDistributionOutputs;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.CapacityInput;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.CapacityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.GetCapacityPerHourService;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.BacklogProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.ProcessParams;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.WavingBacklogProjectionUseCase;
import com.mercadolibre.planning.model.api.web.controller.projection.request.CurrentBacklog;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class WavingBacklogProjectionUseCaseTest {

  @InjectMocks
  private WavingBacklogProjectionUseCase wavingBacklogProjection;

  @Mock
  private GetCapacityPerHourService getCapacityUseCase;

  @Test
  public void createWavingProcessParams() {
    // GIVEN
    final BacklogProjectionInput input = mockBacklogProjectionInput(
        List.of(WAVING, PICKING, PACKING),
        List.of(new CurrentBacklog(WAVING, 0),
                new CurrentBacklog(PICKING, 3000),
                new CurrentBacklog(PACKING, 1110)),
        A_FIXED_DATE.plusHours(4));

    final List<CapacityInput> capacities = CapacityInput.fromEntityOutputs(input.getThroughputs());
    when(getCapacityUseCase.execute(FBM_WMS_OUTBOUND, capacities))
        .thenReturn(getMinCapacity().stream()
                        .map(entityOutput -> new CapacityOutput(
                            entityOutput.getDate(), null, entityOutput.getRoundedValue()))
                        .collect(toList()));

    // WHEN
    final ProcessParams processParams = wavingBacklogProjection.execute(null, input);

    // THEN
    assertEquals(WAVING, processParams.getProcessName());
    assertEquals(0, processParams.getCurrentBacklog());
    assertNull(processParams.getProcessedUnitsByDate());
    assertCapacityByDate(processParams.getCapacityByDate(), getMinCapacity());
    assertPlanningUnits(processParams.getPlanningUnitsByDate());
  }

  @ParameterizedTest
  @MethodSource("getSupportedProcesses")
  public void supportsWavingProcess(final ProcessName processName, final boolean isSupported) {
    // WHEN
    final boolean result = wavingBacklogProjection.supportsProcessName(processName);

    // THEN
    assertEquals(isSupported, result);
  }

  private void assertPlanningUnits(final Map<ZonedDateTime, Long> planningUnitsByDate) {
    final Map<ZonedDateTime, Long> wantedSales = mockPlanningDistributionOutputs().stream()
        .collect(
            toMap(
                output -> ignoreMinutes(ofInstant(output.getDateIn(), UTC)),
                output -> Math.round(output.getTotal()),
                Long::sum)
        );

    assertEquals(wantedSales, planningUnitsByDate);
  }

  private static Stream<Arguments> getSupportedProcesses() {
    return Stream.of(
        Arguments.of(WAVING, true),
        Arguments.of(PICKING, false),
        Arguments.of(PACKING, false)
    );
  }
}
