package com.mercadolibre.planning.model.api.projection.waverless;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mercadolibre.flow.projection.tools.services.entities.context.PiecewiseUpstream;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.OrderedBacklogByDate;
import com.mercadolibre.flow.projection.tools.services.entities.process.ParallelProcess;
import com.mercadolibre.flow.projection.tools.services.entities.process.SimpleProcess;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class PickingProjectionBuilderTest {

  private static final Instant DATE_1 = Instant.parse("2023-02-17T10:00:00Z");

  private static final Instant DATE_2 = Instant.parse("2023-02-17T11:00:00Z");

  private static final Instant DATE_OUT_1 = Instant.parse("2023-02-17T12:00:00Z");

  private static final Instant DATE_OUT_2 = Instant.parse("2023-02-17T13:00:00Z");

  @Test
  @DisplayName("when building graph then return one processor for picking")
  void testGraphBuilding() {
    // GIVEN
    final var processPaths = List.of(TOT_MONO, TOT_MULTI_BATCH);

    // WHEN
    final var graph = PickingProjectionBuilder.buildGraph(processPaths);

    // THEN
    assertEquals("picking", graph.getName());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("when building graph without process paths then an exception should be thrown")
  void testGraphBuildingWithError(List<ProcessPath> processPaths) {
    // WHEN - THEN
    assertThrows(
        IllegalArgumentException.class, () -> PickingProjectionBuilder.buildGraph(processPaths)
    );
  }

  @Test
  @DisplayName("when building contexts then return one context per process path and one for picking")
  void testContextHolderBuilding() {
    // GIVEN
    final var currentBacklog = Map.of(
        TOT_MONO, Map.of(DATE_OUT_1, 100, DATE_OUT_2, 200),
        NON_TOT_MONO, Map.of(DATE_OUT_1, 200, DATE_OUT_2, 300)
    );

    final var throughput = Map.of(
        TOT_MONO, Map.of(DATE_1, 200, DATE_2, 500),
        NON_TOT_MONO, Map.of(DATE_1, 600, DATE_2, 600)
    );

    // WHEN
    final var holder = PickingProjectionBuilder.buildContextHolder(currentBacklog, throughput);

    // THEN
    assertEquals(3, holder.getProcessContextByProcessName().size());

    assertNotNull(holder.getProcessContextByProcessName("picking"));
    assertNotNull(holder.getProcessContextByProcessName(TOT_MONO.toString()));
    assertNotNull(holder.getProcessContextByProcessName(NON_TOT_MONO.toString()));

    assertEquals(ParallelProcess.Context.class, holder.getProcessContextByProcessName("picking").getClass());
    assertEquals(SimpleProcess.Context.class, holder.getProcessContextByProcessName(TOT_MONO.toString()).getClass());
    assertEquals(SimpleProcess.Context.class, holder.getProcessContextByProcessName(NON_TOT_MONO.toString()).getClass());

    final var totMonoContext = (SimpleProcess.Context) holder.getProcessContextByProcessName(TOT_MONO.toString());
    assertEquals(300, totMonoContext.getInitialBacklog().total());
    assertEquals(200, totMonoContext.getTph().availableBetween(DATE_1, DATE_2));

    final var nonTotMonoContext = (SimpleProcess.Context) holder.getProcessContextByProcessName(NON_TOT_MONO.toString());
    assertEquals(500, nonTotMonoContext.getInitialBacklog().total());
    assertEquals(600, nonTotMonoContext.getTph().availableBetween(DATE_1, DATE_2));
  }

  @Test
  @DisplayName("when running a projection then graph and contexts must be compatible")
  void testSimpleProjection() {
    // GIVEN
    final var currentBacklog = Map.of(
        TOT_MONO, Map.of(DATE_OUT_1, 100, DATE_OUT_2, 200),
        NON_TOT_MONO, Map.of(DATE_OUT_1, 200, DATE_OUT_2, 300)
    );

    final var throughput = Map.of(
        TOT_MONO, Map.of(DATE_1, 300, DATE_2, 500),
        NON_TOT_MONO, Map.of(DATE_1, 600, DATE_2, 600)
    );

    final var holder = PickingProjectionBuilder.buildContextHolder(currentBacklog, throughput);

    final var processPaths = List.of(TOT_MONO, NON_TOT_MONO);

    final var graph = PickingProjectionBuilder.buildGraph(processPaths);

    final var wave = Map.of(
        DATE_1,
        new OrderedBacklogByProcessPath(
            Map.of(
                TOT_MONO, new OrderedBacklogByDate(Map.of(DATE_OUT_1, new OrderedBacklogByDate.Quantity(100)))
            )
        ),
        DATE_2,
        new OrderedBacklogByProcessPath(
            Map.of(
                TOT_MONO, new OrderedBacklogByDate(Map.of(DATE_OUT_1, new OrderedBacklogByDate.Quantity(100)))
            )
        )
    );

    final var upstream = new PiecewiseUpstream(wave);

    // WHEN
    final var result = graph.accept(holder, upstream, List.of(DATE_1, DATE_2));

    // THEN
    assertNotNull(result);

    final var totMonoContext = (SimpleProcess.Context) result.getProcessContextByProcessName(TOT_MONO.toString());

    assertEquals(1, totMonoContext.getProcessedBacklog().size());
    final var processedBacklog = totMonoContext.getProcessedBacklog().get(0);
    assertEquals(DATE_1, processedBacklog.getStartDate());
    assertEquals(DATE_2, processedBacklog.getEndDate());
    assertEquals(300, processedBacklog.getBacklog().total());
  }
}
