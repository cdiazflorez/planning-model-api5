package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentHeadcountProductivityRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.HeadcountProductivityRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.HeadcountProductivityView;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentHeadcountProductivity;
import com.mercadolibre.planning.model.api.domain.usecase.GetProductivityEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.EntityOutput;
import com.mercadolibre.planning.model.api.web.controller.request.EntityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.DateUtils.getForecastWeeks;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockCurrentProdEntity;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockGetProductivityEntityInput;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.FORECAST;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.SIMULATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetProductivityEntityUseCaseTest {

    @Mock
    private HeadcountProductivityRepository productivityRepository;

    @Mock
    private CurrentHeadcountProductivityRepository currentProductivityRepository;

    @InjectMocks
    private GetProductivityEntityUseCase getProductivityEntityUseCase;

    @Test
    @DisplayName("Get productivity entity when source is forecast")
    public void testGetProductivityOk() {
        // GIVEN
        final GetEntityInput input = mockGetProductivityEntityInput(FORECAST);
        when(productivityRepository.findByWarehouseIdAndWorkflowAndProcessName(
                "ARBA01",
                FBM_WMS_OUTBOUND.name(),
                List.of(PICKING.name(), PACKING.name()),
                input.getDateFrom(),
                input.getDateTo(),
                getForecastWeeks(input.getDateFrom(), input.getDateTo()))
        ).thenReturn(productivities());

        // WHEN
        final List<EntityOutput> output = getProductivityEntityUseCase.execute(input);

        // THEN
        whenTestOutpoutResponse(output, false);
    }

    @Test
    @DisplayName("Get productivity entity when source is simulation")
    public void testGetProductivityFromSourceSimulation() {
        // GIVEN
        final GetEntityInput input = mockGetProductivityEntityInput(SIMULATION);
        final CurrentHeadcountProductivity currentProd = mockCurrentProdEntity();

        // WHEN
        when(productivityRepository.findByWarehouseIdAndWorkflowAndProcessName(
                "ARBA01",
                FBM_WMS_OUTBOUND.name(),
                List.of(PICKING.name(), PACKING.name()),
                input.getDateFrom(),
                input.getDateTo(),
                getForecastWeeks(input.getDateFrom(), input.getDateTo()))
        ).thenReturn(productivities());

        when(currentProductivityRepository
                .findSimulationByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                        currentProd.getLogisticCenterId(),
                        FBM_WMS_OUTBOUND,
                        List.of(PICKING, PACKING),
                        input.getDateFrom(),
                        input.getDateTo()
                )
        ).thenReturn(currentProductivities());

        final List<EntityOutput> output = getProductivityEntityUseCase.execute(input);

        // THEN
        assertThat(output).isNotEmpty();
        whenTestOutpoutResponse(output, true);
    }

    @ParameterizedTest
    @DisplayName("Only supports productivity entity")
    @MethodSource("getSupportedEntitites")
    public void testSupportEntityTypeOk(final EntityType entityType,
                                        final boolean shouldBeSupported) {
        // WHEN
        final boolean isSupported = getProductivityEntityUseCase.supportsEntityType(entityType);

        // THEN
        assertEquals(shouldBeSupported, isSupported);
    }

    private List<HeadcountProductivityView> productivities() {
        return List.of(
                new HeadcountProductivityViewImpl(PICKING,
                        80, UNITS_PER_HOUR, Date.from(A_DATE_UTC.toInstant())),
                new HeadcountProductivityViewImpl(PICKING,
                        85, UNITS_PER_HOUR, Date.from(A_DATE_UTC.plusHours(1).toInstant())),
                new HeadcountProductivityViewImpl(PACKING,
                        90, UNITS_PER_HOUR, Date.from(A_DATE_UTC.toInstant())),
                new HeadcountProductivityViewImpl(PACKING,
                        92, UNITS_PER_HOUR, Date.from(A_DATE_UTC.plusHours(1).toInstant()))
        );
    }

    private List<CurrentHeadcountProductivity> currentProductivities() {
        return List.of(
                CurrentHeadcountProductivity
                        .builder()
                        .abilityLevel(1L)
                        .date(A_DATE_UTC)
                        .isActive(true)
                        .productivity(68)
                        .productivityMetricUnit(UNITS_PER_HOUR)
                        .processName(PICKING)
                        .logisticCenterId(WAREHOUSE_ID)
                        .workflow(FBM_WMS_OUTBOUND)
                        .build()
        );
    }


    private static Stream<Arguments> getSupportedEntitites() {
        return Stream.of(
                Arguments.of(PRODUCTIVITY, true),
                Arguments.of(HEADCOUNT, false),
                Arguments.of(THROUGHPUT, false)
        );
    }


    private void whenTestOutpoutResponse(final List<EntityOutput> output,
                                         final boolean isSimulation) {
        assertEquals(4, output.size());
        final EntityOutput output1 = output.get(0);
        assertEquals(PICKING, output1.getProcessName());
        assertEquals(isSimulation ? 68 : 80, output1.getValue());
        assertEquals(UNITS_PER_HOUR, output1.getMetricUnit());
        assertEquals(FORECAST, output1.getSource());
        assertEquals(FBM_WMS_OUTBOUND, output1.getWorkflow());

        final EntityOutput output2 = output.get(1);
        assertEquals(PICKING, output2.getProcessName());
        assertEquals(85, output2.getValue());
        assertEquals(UNITS_PER_HOUR, output2.getMetricUnit());
        assertEquals(FORECAST, output2.getSource());
        assertEquals(FBM_WMS_OUTBOUND, output2.getWorkflow());

        final EntityOutput output3 = output.get(2);
        assertEquals(PACKING, output3.getProcessName());
        assertEquals(90, output3.getValue());
        assertEquals(UNITS_PER_HOUR, output3.getMetricUnit());
        assertEquals(FORECAST, output3.getSource());
        assertEquals(FBM_WMS_OUTBOUND, output3.getWorkflow());

        final EntityOutput output4 = output.get(3);
        assertEquals(PACKING, output4.getProcessName());
        assertEquals(92, output4.getValue());
        assertEquals(UNITS_PER_HOUR, output4.getMetricUnit());
        assertEquals(FORECAST, output4.getSource());
        assertEquals(FBM_WMS_OUTBOUND, output4.getWorkflow());
    }

}