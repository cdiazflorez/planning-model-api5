package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.GetPlanningDistributionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.GetThroughputEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.Backlog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.CalculateCptProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.ProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.ProjectionOutput;
import com.mercadolibre.planning.model.api.web.controller.request.ProjectionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.mercadolibre.planning.model.api.domain.usecase.output.GetPlanningDistributionOutput.builder;
import static com.mercadolibre.planning.model.api.web.controller.request.ProjectionType.BACKLOG;
import static com.mercadolibre.planning.model.api.web.controller.request.ProjectionType.CPT;
import static java.time.ZonedDateTime.parse;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CalculateCptProjectionUseCaseTest {

    private static final ZonedDateTime DATE_FROM_10 = parse("2020-01-01T10:00:00Z");
    private static final ZonedDateTime DATE_IN_11 = parse("2020-01-01T11:00:00Z");
    private static final ZonedDateTime DATE_OUT_12 = parse("2020-01-01T12:00:00Z");
    private static final ZonedDateTime DATE_OUT_12_30 = parse("2020-01-01T12:30:00Z");
    private static final ZonedDateTime DATE_OUT_13 = parse("2020-01-01T13:00:00Z");
    private static final ZonedDateTime DATE_TO_14 = parse("2020-01-01T14:00:00Z");
    private static final ZonedDateTime DATE_OUT_16 = parse("2020-01-01T16:00:00Z");

    @InjectMocks
    private CalculateCptProjectionUseCase calculateCptProjection;

    @Mock
    private GetThroughputEntityUseCase getThroughput;

    @Mock
    private GetPlanningDistributionUseCase getPlanning;

    @Test
    @DisplayName("The projected end date is the same as the date out and all units were processed")
    public void testSameProjectedEndDateAndDateOut() {
        // GIVEN
        final List<Backlog> backlogs = singletonList(new Backlog(DATE_OUT_12, 100));
        final ProjectionInput input = mockProjectionInput(DATE_FROM_10, DATE_TO_14, backlogs);

        when(getPlanning.execute(mockPlanningInput(input))).thenReturn(
                singletonList(builder()
                        .dateOut(DATE_OUT_12)
                        .dateIn(DATE_IN_11)
                        .total(200)
                        .build()));

        when(getThroughput.execute(mockGetEntityInput(input)))
                .thenReturn(mockThroughputs(DATE_FROM_10, DATE_OUT_12, List.of(100, 200, 200)));

        // WHEN
        final List<ProjectionOutput> projections = calculateCptProjection.execute(input);

        // THEN
        assertEquals(1, projections.size());

        final ProjectionOutput projection = projections.get(0);
        assertEquals(DATE_OUT_12, projection.getDate());
        assertEquals(DATE_OUT_12, projection.getProjectedEndDate());
        assertEquals(0, projection.getRemainingQuantity());
    }

    @Test
    @DisplayName("The projected end date is before the date out and all units were processed")
    public void testProjectedEndDateBeforeDateOut() {
        // GIVEN
        final List<Backlog> backlogs = singletonList(new Backlog(DATE_OUT_12, 100));
        final ProjectionInput input = mockProjectionInput(DATE_FROM_10, DATE_TO_14, backlogs);

        when(getPlanning.execute(mockPlanningInput(input))).thenReturn(
                singletonList(builder()
                        .dateOut(DATE_OUT_12)
                        .dateIn(DATE_IN_11)
                        .total(100)
                        .build()));

        when(getThroughput.execute(mockGetEntityInput(input)))
                .thenReturn(mockThroughputs(DATE_FROM_10, DATE_OUT_12, List.of(100, 200, 200)));

        // WHEN
        final List<ProjectionOutput> projections = calculateCptProjection.execute(input);

        // THEN
        assertEquals(1, projections.size());

        final ProjectionOutput projection = projections.get(0);
        assertEquals(DATE_OUT_12, projection.getDate());
        assertEquals(parse("2020-01-01T11:30:00Z"), projection.getProjectedEndDate());
        assertEquals(0, projection.getRemainingQuantity());
    }

    @Test
    @DisplayName("The projected end date is after the date to, so it returns null")
    public void testProjectedEndDateAfterDateTo() {
        // GIVEN
        final List<Backlog> backlogs = singletonList(new Backlog(DATE_OUT_12, 1000));
        final ProjectionInput input = mockProjectionInput(DATE_FROM_10, DATE_TO_14, backlogs);

        when(getPlanning.execute(mockPlanningInput(input))).thenReturn(
                singletonList(builder()
                        .dateOut(DATE_OUT_12)
                        .dateIn(DATE_IN_11)
                        .total(100)
                        .build()));

        when(getThroughput.execute(mockGetEntityInput(input)))
                .thenReturn(mockThroughputs(DATE_FROM_10, DATE_OUT_12, List.of(100, 200, 200)));

        // WHEN
        final List<ProjectionOutput> projections = calculateCptProjection.execute(input);

        // THEN
        assertEquals(1, projections.size());

        final ProjectionOutput projection = projections.get(0);
        assertEquals(DATE_OUT_12, projection.getDate());
        assertNull(projection.getProjectedEndDate());
        assertEquals(800, projection.getRemainingQuantity());
    }

    @ParameterizedTest
    @DisplayName("The projected end date is after the date out and some units weren't processed")
    @MethodSource("multipleDateOuts")
    public void testProjectedEndDateAfterDateOut(final ZonedDateTime dateOut,
                                                 final int remainingQuantity) {
        // GIVEN
        final List<Backlog> backlogs = singletonList(new Backlog(dateOut, 100));
        final ProjectionInput input = mockProjectionInput(DATE_FROM_10, DATE_TO_14, backlogs);

        when(getPlanning.execute(mockPlanningInput(input))).thenReturn(
                singletonList(builder()
                        .dateOut(dateOut)
                        .dateIn(DATE_IN_11)
                        .total(400)
                        .build()));

        when(getThroughput.execute(mockGetEntityInput(input)))
                .thenReturn(mockThroughputs(DATE_FROM_10, dateOut, List.of(100, 200, 200)));

        // WHEN
        final List<ProjectionOutput> projections = calculateCptProjection.execute(input);

        // THEN
        assertEquals(1, projections.size());

        final ProjectionOutput projection = projections.get(0);
        assertEquals(dateOut, projection.getDate());
        assertEquals(parse("2020-01-01T13:00:00Z"), projection.getProjectedEndDate());
        assertEquals(remainingQuantity, projection.getRemainingQuantity());
    }

    private static Stream<Arguments> multipleDateOuts() {
        return Stream.of(
                arguments(DATE_OUT_12, 200),
                arguments(DATE_OUT_12_30, 100)
        );
    }

    @Test
    @DisplayName("The capacity is shared among al date outs")
    public void testMultipleDateOuts() {
        // GIVEN
        final List<Backlog> backlogs = List.of(
                new Backlog(DATE_OUT_12, 100),
                new Backlog(DATE_OUT_13, 150)
        );

        final ProjectionInput input = mockProjectionInput(DATE_FROM_10, DATE_OUT_16, backlogs);

        when(getPlanning.execute(mockPlanningInput(input))).thenReturn(
                List.of(
                        builder().dateOut(DATE_OUT_12).dateIn(DATE_IN_11).total(100).build(),
                        builder().dateOut(DATE_OUT_13).dateIn(DATE_IN_11).total(350).build()));

        when(getThroughput.execute(mockGetEntityInput(input)))
                .thenReturn(mockThroughputs(DATE_FROM_10, DATE_OUT_16.plusHours(2),
                        List.of(200, 200, 100, 100, 100, 100, 100, 100, 100)));

        // WHEN
        final List<ProjectionOutput> projections = calculateCptProjection.execute(input);

        // THEN
        assertEquals(2, projections.size());

        final ProjectionOutput projection1 = projections.get(0);
        assertEquals(DATE_OUT_12, projection1.getDate());
        assertEquals(parse("2020-01-01T11:30:00Z"), projection1.getProjectedEndDate());
        assertEquals(0, projection1.getRemainingQuantity());

        final ProjectionOutput projection2 = projections.get(1);
        assertEquals(DATE_OUT_13, projection2.getDate());
        assertEquals(parse("2020-01-01T15:00:00Z"), projection2.getProjectedEndDate());
        assertEquals(200, projection2.getRemainingQuantity());
    }

    @ParameterizedTest
    @DisplayName("Only supports cpt type")
    @MethodSource("getSupportedProjectionTypes")
    public void testSupportEntityTypeOk(final ProjectionType projectionType,
                                        final boolean shouldBeSupported) {
        // WHEN
        final boolean isSupported = calculateCptProjection.supportsProjectionType(projectionType);

        // THEN
        assertEquals(shouldBeSupported, isSupported);
    }

    private static Stream<Arguments> getSupportedProjectionTypes() {
        return Stream.of(
                Arguments.of(CPT, true),
                Arguments.of(BACKLOG, false)
        );
    }


    private List<EntityOutput> mockThroughputs(final ZonedDateTime dateFrom,
                                               final Temporal dateTo,
                                               final List<Integer> values) {

        final List<EntityOutput> entityOutputs = new ArrayList<>();

        for (int i = 0; i <= HOURS.between(dateFrom, dateTo); i++) {
            entityOutputs.add(
                    EntityOutput.builder()
                            .date(dateFrom.plusHours(i))
                            .value(values.get(i))
                            .build());
        }

        return entityOutputs;
    }

    private GetPlanningDistributionInput mockPlanningInput(final ProjectionInput projectionInput) {
        return GetPlanningDistributionInput.builder()
                .warehouseId(projectionInput.getWarehouseId())
                .dateFrom(projectionInput.getDateFrom())
                .dateTo(projectionInput.getDateTo())
                .workflow(Workflow.FBM_WMS_OUTBOUND)
                .build();
    }

    private GetEntityInput mockGetEntityInput(final ProjectionInput projectionInput) {
        return GetEntityInput.builder()
                .warehouseId(projectionInput.getWarehouseId())
                .dateFrom(projectionInput.getDateFrom())
                .dateTo(projectionInput.getDateTo())
                .processName(projectionInput.getProcessName())
                .workflow(Workflow.FBM_WMS_OUTBOUND)
                .build();
    }

    private ProjectionInput mockProjectionInput(final ZonedDateTime dateFrom,
                                                final ZonedDateTime dateTo,
                                                final List<Backlog> backlogs) {
        return ProjectionInput.builder()
                .warehouseId("ARBA01")
                .type(CPT)
                .workflow(Workflow.FBM_WMS_OUTBOUND)
                .processName(List.of(ProcessName.PICKING, ProcessName.PACKING))
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .backlog(backlogs)
                .build();
    }
}
