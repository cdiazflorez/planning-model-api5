package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.domain.usecase.cptbywarehouse.GetCptByWarehouseOutput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.Backlog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CalculateCptProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptCalculationOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionInput;
import com.mercadolibre.planning.model.api.util.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput.builder;
import static com.mercadolibre.planning.model.api.util.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType.CPT;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType.DEFERRAL;
import static java.time.ZonedDateTime.parse;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
public class CalculateGetCptProjectionUseCaseTest {

    private static final ZonedDateTime DATE_10 = parse("2020-01-01T10:00:00Z");
    private static final ZonedDateTime DATE_IN_11 = parse("2020-01-01T11:00:00Z");
    private static final ZonedDateTime DATE_OUT_12 = parse("2020-01-01T12:00:00Z");
    private static final ZonedDateTime DATE_OUT_12_30 = parse("2020-01-01T12:30:00Z");
    private static final ZonedDateTime DATE_OUT_13 = parse("2020-01-01T13:00:00Z");
    private static final ZonedDateTime DATE_TO_14 = parse("2020-01-01T14:00:00Z");
    private static final ZonedDateTime DATE_OUT_16 = parse("2020-01-01T16:00:00Z");

    @InjectMocks
    private CalculateCptProjectionUseCase calculateCptProjection;

    private MockedStatic<DateUtils> mockedDates;

    @BeforeEach
    public void setUp() {
        mockedDates = mockStatic(DateUtils.class);
        mockedDates.when(DateUtils::getCurrentUtcDate).thenReturn(DATE_10);
        mockedDates.when(() -> DateUtils.ignoreMinutes(any())).thenCallRealMethod();
    }

    @AfterEach
    public void tearDown() {
        mockedDates.close();
    }

    @Test
    @DisplayName("The projected end date is the same as the date out and all units were processed")
    public void testSameProjectedEndDateAndDateOut() {

        // GIVEN
        final List<Backlog> backlogs = singletonList(new Backlog(DATE_OUT_12, 100));

        final List<GetPlanningDistributionOutput> planningUnits = singletonList(builder()
                .dateOut(DATE_OUT_12)
                .dateIn(DATE_IN_11)
                .total(200)
                .build());

        final CptProjectionInput input = CptProjectionInput.builder()
                .dateFrom(DATE_10)
                .dateTo(DATE_TO_14)
                .planningUnits(planningUnits)
                .capacity(mockCapacity(DATE_OUT_12, List.of(100, 200, 200)))
                .logisticCenterId(WAREHOUSE_ID)
                .backlog(backlogs)
                .projectionType(CPT)
                .cptByWarehouse(List.of(
                        GetCptByWarehouseOutput.builder().date(DATE_OUT_12).build()))
                .currentDate(getCurrentUtcDate())
                .build();

        // WHEN
        final List<CptCalculationOutput> projections = calculateCptProjection.execute(input);

        // THEN
        assertEquals(1, projections.size());
        final CptCalculationOutput projection = projections.get(0);
        assertEquals(DATE_OUT_12, projection.getDate());
        assertEquals(DATE_OUT_13, projection.getProjectedEndDate());
        assertEquals(0, projection.getRemainingQuantity());
    }

    @Test
    @DisplayName("The projected end date is before the date out and all units were processed")
    public void testProjectedEndDateBeforeDateOut() {
        // GIVEN
        final List<Backlog> backlogs = singletonList(new Backlog(DATE_OUT_12, 100));
        final List<GetPlanningDistributionOutput> planningUnits = singletonList(builder()
                .dateOut(DATE_OUT_12)
                .dateIn(DATE_IN_11)
                .total(100)
                .build());

        final CptProjectionInput input = CptProjectionInput.builder()
                .dateFrom(DATE_10)
                .dateTo(DATE_TO_14)
                .planningUnits(planningUnits)
                .capacity(mockCapacity(DATE_OUT_12, List.of(100, 200, 200)))
                .backlog(backlogs)
                .logisticCenterId(WAREHOUSE_ID)
                .projectionType(CPT)
                .cptByWarehouse(List.of(
                        GetCptByWarehouseOutput.builder().date(DATE_OUT_12).build()))
                .currentDate(getCurrentUtcDate())
                .build();

        // WHEN
        final List<CptCalculationOutput> projections = calculateCptProjection.execute(input);

        // THEN
        assertEquals(1, projections.size());

        final CptCalculationOutput projection = projections.get(0);
        assertEquals(DATE_OUT_12, projection.getDate());
        assertEquals(parse("2020-01-01T12:30:00Z"), projection.getProjectedEndDate());
        assertEquals(0, projection.getRemainingQuantity());
    }

    @Test
    @DisplayName("The projected end date is after the date to, so it returns null")
    public void testProjectedEndDateAfterDateTo() {
        // GIVEN
        final List<Backlog> backlogs = singletonList(new Backlog(DATE_OUT_12, 1000));
        final List<GetPlanningDistributionOutput> planningUnits = singletonList(builder()
                .dateOut(DATE_OUT_12)
                .dateIn(DATE_IN_11)
                .total(100)
                .build());

        final CptProjectionInput input = CptProjectionInput.builder()
                .dateFrom(DATE_10)
                .dateTo(DATE_TO_14)
                .planningUnits(planningUnits)
                .capacity(mockCapacity(DATE_OUT_12, List.of(100, 200, 200)))
                .backlog(backlogs)
                .logisticCenterId(WAREHOUSE_ID)
                .projectionType(CPT)
                .cptByWarehouse(List.of(
                        GetCptByWarehouseOutput.builder().date(DATE_OUT_12).build()))
                .currentDate(getCurrentUtcDate())
                .build();

        // WHEN
        final List<CptCalculationOutput> projections = calculateCptProjection.execute(input);

        // THEN
        assertEquals(1, projections.size());

        final CptCalculationOutput projection = projections.get(0);
        assertEquals(DATE_OUT_12, projection.getDate());
        assertNull(projection.getProjectedEndDate());
        assertEquals(700, projection.getRemainingQuantity());
    }

    @ParameterizedTest
    @DisplayName("The projected end date is after the date out and some units weren't processed")
    @MethodSource("multipleDateOuts")
    public void testProjectedEndDateAfterDateOut(final ZonedDateTime dateOut,
                                                 final int remainingQuantity) {
        // GIVEN
        final List<Backlog> backlogs = singletonList(new Backlog(dateOut, 100));
        final List<GetPlanningDistributionOutput> planningUnits = singletonList(builder()
                .dateOut(dateOut)
                .dateIn(DATE_10)
                .total(400)
                .build());

        final CptProjectionInput input = CptProjectionInput.builder()
                .dateFrom(DATE_10)
                .dateTo(DATE_TO_14)
                .planningUnits(planningUnits)
                .capacity(mockCapacity(dateOut, List.of(100, 200, 200)))
                .backlog(backlogs)
                .logisticCenterId(WAREHOUSE_ID)
                .projectionType(CPT)
                .cptByWarehouse(List.of(
                        GetCptByWarehouseOutput.builder().date(dateOut).build()))
                .currentDate(getCurrentUtcDate())
                .build();

        // WHEN
        final List<CptCalculationOutput> projections = calculateCptProjection.execute(input);

        // THEN
        assertEquals(1, projections.size());

        final CptCalculationOutput projection = projections.get(0);
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

        final List<GetPlanningDistributionOutput> planningUnits = List.of(
                builder().dateOut(DATE_OUT_12).dateIn(DATE_IN_11).total(100).build(),
                builder().dateOut(DATE_OUT_13).dateIn(DATE_IN_11).total(350).build());

        final CptProjectionInput input = CptProjectionInput.builder()
                .dateFrom(DATE_10)
                .dateTo(DATE_OUT_16)
                .planningUnits(planningUnits)
                .capacity(mockCapacity(DATE_OUT_16.plusHours(2),
                        List.of(200, 200, 200, 100, 100, 100, 100, 100, 100)))
                .backlog(backlogs)
                .logisticCenterId(WAREHOUSE_ID)
                .projectionType(CPT)
                .cptByWarehouse(List.of(
                        GetCptByWarehouseOutput.builder().date(DATE_OUT_12).build(),
                        GetCptByWarehouseOutput.builder().date(DATE_OUT_13).build()))
                .currentDate(getCurrentUtcDate())
                .build();

        // WHEN
        final List<CptCalculationOutput> projections = calculateCptProjection.execute(input);

        // THEN
        assertEquals(2, projections.size());

        final CptCalculationOutput projection1 = projections.get(0);
        assertEquals(DATE_OUT_12, projection1.getDate());
        assertEquals(parse("2020-01-01T12:30:00Z"), projection1.getProjectedEndDate());
        assertEquals(0, projection1.getRemainingQuantity());

        final CptCalculationOutput projection2 = projections.get(1);
        assertEquals(DATE_OUT_13, projection2.getDate());
        assertEquals(parse("2020-01-01T15:30:00Z"), projection2.getProjectedEndDate());
        assertEquals(250, projection2.getRemainingQuantity());
    }

    @Test
    @DisplayName("GetCptByWarehouseOutput without items shouldn't be returned")
    public void testEmptyCpt() {
        final CptProjectionInput input = CptProjectionInput.builder()
                .dateFrom(DATE_10)
                .dateTo(DATE_OUT_16)
                .capacity(mockCapacity(DATE_OUT_16.plusHours(2),
                        List.of(200, 200, 100, 100, 100, 100, 100, 100, 100)))
                .planningUnits(emptyList())
                .logisticCenterId(WAREHOUSE_ID)
                .projectionType(CPT)
                .cptByWarehouse(emptyList())
                .currentDate(getCurrentUtcDate())
                .build();

        // WHEN
        final List<CptCalculationOutput> projections = calculateCptProjection.execute(input);

        // THEN
        assertTrue(projections.isEmpty());
    }

    @Test
    @DisplayName("Recalculate de projection if has new items")
    public void testRecalculateProjectionDate() {
        final CptProjectionInput input = CptProjectionInput.builder()
                .dateFrom(DATE_10)
                .dateTo(DATE_OUT_16)
                .planningUnits(List.of(
                        builder().dateOut(DATE_OUT_16).dateIn(DATE_IN_11.minusHours(1))
                                .total(100).build(),
                        builder().dateOut(DATE_OUT_16).dateIn(DATE_IN_11)
                                .total(100).build(),
                        builder().dateOut(DATE_OUT_16).dateIn(DATE_IN_11.plusHours(1))
                                .total(50).build(),
                        builder().dateOut(DATE_OUT_16).dateIn(DATE_IN_11.plusHours(2))
                                .total(50).build(),
                        builder().dateOut(DATE_OUT_16).dateIn(DATE_IN_11.plusHours(3))
                                .total(50).build(),
                        builder().dateOut(DATE_OUT_16).dateIn(DATE_IN_11.plusHours(4))
                                .total(50).build(),
                        builder().dateOut(DATE_OUT_16).dateIn(DATE_IN_11.plusHours(5))
                                .total(50).build()))
                .capacity(mockCapacity(DATE_OUT_16.plusHours(2),
                        List.of(200, 200, 20, 20, 20, 20, 20, 20, 20)))
                .logisticCenterId(WAREHOUSE_ID)
                .projectionType(CPT)
                .cptByWarehouse(List.of(
                        GetCptByWarehouseOutput.builder().date(DATE_OUT_16).build()))
                .currentDate(getCurrentUtcDate())
                .build();

        // WHEN
        final List<CptCalculationOutput> projections = calculateCptProjection.execute(input);

        // THEN
        assertEquals(1, projections.size());

        final CptCalculationOutput projection1 = projections.get(0);
        assertEquals(DATE_OUT_16, projection1.getDate());
        assertNull(projection1.getProjectedEndDate());
        assertEquals(170, projection1.getRemainingQuantity());
    }

    @Test
    @DisplayName("CPT coming from backlog but not present in Forecast should be calculated anyway")
    public void testCptIsNotPresentInForecast() {
        // GIVEN
        final List<Backlog> backlogs = List.of(
                new Backlog(DATE_OUT_12, 100),
                new Backlog(DATE_OUT_12_30, 150),
                new Backlog(DATE_OUT_13, 200)
        );

        final List<GetPlanningDistributionOutput> planningUnits = List.of(
                builder().dateOut(DATE_OUT_12).dateIn(DATE_IN_11).total(100).build(),
                builder().dateOut(DATE_OUT_13).dateIn(DATE_IN_11).total(350).build());

        final CptProjectionInput input = CptProjectionInput.builder()
                .dateFrom(DATE_10)
                .dateTo(DATE_OUT_16)
                .planningUnits(planningUnits)
                .capacity(mockCapacity(DATE_OUT_16.plusHours(2),
                        List.of(200, 200, 200, 100, 100, 100, 100, 100, 100)))
                .backlog(backlogs)
                .logisticCenterId(WAREHOUSE_ID)
                .projectionType(CPT)
                .cptByWarehouse(List.of(
                        GetCptByWarehouseOutput.builder().date(DATE_OUT_12).build(),
                        GetCptByWarehouseOutput.builder().date(DATE_OUT_12_30).build(),
                        GetCptByWarehouseOutput.builder().date(DATE_OUT_13).build()))
                .currentDate(getCurrentUtcDate())
                .build();

        // WHEN
        final List<CptCalculationOutput> projections = calculateCptProjection.execute(input);

        // THEN
        assertEquals(3, projections.size());

        final CptCalculationOutput projection1 = projections.get(0);
        assertEquals(DATE_OUT_12, projection1.getDate());
        assertEquals(parse("2020-01-01T12:30:00Z"), projection1.getProjectedEndDate());
        assertEquals(0, projection1.getRemainingQuantity());

        final CptCalculationOutput projection2 = projections.get(1);
        assertEquals(DATE_OUT_12_30, projection2.getDate());
        assertEquals(parse("2020-01-01T11:15:00Z"), projection2.getProjectedEndDate());
        assertEquals(0, projection2.getRemainingQuantity());

        final CptCalculationOutput projection3 = projections.get(2);
        assertEquals(DATE_OUT_13, projection3.getDate());
        assertEquals(parse("2020-01-01T16:00:00Z"), projection3.getProjectedEndDate());
        assertEquals(300, projection3.getRemainingQuantity());
    }

    @Test
    @DisplayName("CPT is deferred")
    public void testCptIsDeferred() {
        // GIVEN
        final List<Backlog> backlogs = List.of(
                new Backlog(DATE_OUT_12, 100),
                new Backlog(DATE_OUT_12_30, 150),
                new Backlog(DATE_OUT_13, 200)
        );

        final List<GetPlanningDistributionOutput> planningUnits = List.of(
                builder()
                        .dateOut(DATE_OUT_12)
                        .dateIn(DATE_IN_11)
                        .total(100)
                        .isDeferred(false)
                        .build(),
                builder()
                        .dateOut(DATE_OUT_13)
                        .dateIn(DATE_IN_11)
                        .total(350)
                        .isDeferred(true)
                        .build(),
                builder()
                        .dateOut(DATE_OUT_13)
                        .dateIn(DATE_IN_11.plusHours(1))
                        .isDeferred(true)
                        .total(350)
                        .build()
        );

        final CptProjectionInput input = CptProjectionInput.builder()
                .dateFrom(DATE_10)
                .dateTo(DATE_OUT_16)
                .planningUnits(planningUnits)
                .capacity(mockCapacity(DATE_OUT_16.plusHours(2),
                        List.of(200, 200, 200, 100, 100, 100, 100, 100, 100)))
                .backlog(backlogs)
                .logisticCenterId(WAREHOUSE_ID)
                .projectionType(CPT)
                .cptByWarehouse(List.of(
                        GetCptByWarehouseOutput.builder().date(DATE_OUT_12).build(),
                        GetCptByWarehouseOutput.builder().date(DATE_OUT_12_30).build(),
                        GetCptByWarehouseOutput.builder().date(DATE_OUT_13).build()))
                .currentDate(getCurrentUtcDate())
                .build();

        // WHEN
        final List<CptCalculationOutput> projections = calculateCptProjection.execute(input);

        // THEN
        assertEquals(3, projections.size());

        final CptCalculationOutput projection1 = projections.get(0);
        assertEquals(DATE_OUT_12, projection1.getDate());

        final CptCalculationOutput projection2 = projections.get(1);
        assertEquals(DATE_OUT_12_30, projection2.getDate());

        final CptCalculationOutput projection3 = projections.get(2);
        assertEquals(DATE_OUT_13, projection3.getDate());
    }

    @Test
    @DisplayName("Get Deferral Projection's remaining quantity OK")
    public void testDeferralProjectionOk() {
        // GIVEN
        final List<Backlog> backlogs = singletonList(new Backlog(DATE_OUT_12, 100));
        final ZonedDateTime date13 = DATE_10.plusHours(3);

        final CptProjectionInput input = CptProjectionInput.builder()
                .dateFrom(DATE_10)
                .dateTo(date13)
                .planningUnits(emptyList())
                .capacity(mockCapacity(date13, List.of(50, 50, 25, 40)))
                .backlog(backlogs)
                .logisticCenterId(WAREHOUSE_ID)
                .projectionType(DEFERRAL)
                .cptByWarehouse(List.of(
                        GetCptByWarehouseOutput.builder().date(DATE_OUT_12).build()))
                .currentDate(getCurrentUtcDate())
                .build();

        // WHEN
        final List<CptCalculationOutput> projections = calculateCptProjection.execute(input);

        // THEN
        assertEquals(1, projections.size());

        final CptCalculationOutput projection = projections.get(0);
        assertEquals(DATE_OUT_12, projection.getDate());
        assertEquals(DATE_OUT_12, projection.getProjectedEndDate());
        assertEquals(0, projection.getRemainingQuantity());
    }

    @Test
    @DisplayName("Get Projection's all cpt")
    public void testAllCpt() {
        // GIVEN
        final List<Backlog> backlogs = singletonList(new Backlog(DATE_OUT_12, 100));
        final ZonedDateTime date13 = DATE_10.plusHours(3);

        final CptProjectionInput input = CptProjectionInput.builder()
                .dateFrom(DATE_10)
                .dateTo(date13)
                .planningUnits(List.of(
                        builder().dateIn(DATE_OUT_12).dateOut(DATE_OUT_12).total(0).build(),
                        builder().dateIn(DATE_OUT_12_30).dateOut(DATE_OUT_12_30).total(0).build(),
                        builder().dateIn(DATE_OUT_13).dateOut(DATE_OUT_13).total(0).build()))
                .capacity(mockCapacity(date13, List.of(50, 50, 25, 40)))
                .backlog(backlogs)
                .logisticCenterId(WAREHOUSE_ID)
                .projectionType(DEFERRAL)
                .cptByWarehouse(List.of(
                        GetCptByWarehouseOutput.builder().date(DATE_OUT_12).build(),
                        GetCptByWarehouseOutput.builder().date(DATE_OUT_12_30).build(),
                        GetCptByWarehouseOutput.builder().date(DATE_OUT_13).build()))
                .currentDate(getCurrentUtcDate())
                .build();

        // WHEN
        final List<CptCalculationOutput> projections = calculateCptProjection.execute(input);

        // THEN
        assertEquals(3, projections.size());

        final CptCalculationOutput projection = projections.get(0);
        assertEquals(DATE_OUT_12, projection.getDate());
        assertEquals(DATE_OUT_12, projection.getProjectedEndDate());
        assertEquals(0, projection.getRemainingQuantity());
    }

    private Map<ZonedDateTime, Integer> mockCapacity(final Temporal dateTo,
                                                     final List<Integer> values) {

        final Map<ZonedDateTime, Integer> capacity = new TreeMap<>();

        for (int i = 0; i <= HOURS.between(DATE_10, dateTo); i++) {
            capacity.put(DATE_10.plusHours(i), values.get(i));
        }
        return capacity;
    }
}