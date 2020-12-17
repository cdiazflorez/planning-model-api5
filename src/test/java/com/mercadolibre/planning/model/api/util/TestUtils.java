package com.mercadolibre.planning.model.api.util;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ForecastMetadataView;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentHeadcountProductivity;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastMetadata;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistributionMetadata;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.entities.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.input.GetHeadcountInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.input.GetProductivityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.output.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.output.ProductivityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.input.CreateForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetForecastMetadataInput;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetSuggestedWavesInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.usecase.ForecastMetadataViewImpl;
import com.mercadolibre.planning.model.api.usecase.PlanningDistributionViewImpl;
import com.mercadolibre.planning.model.api.web.controller.request.AreaRequest;
import com.mercadolibre.planning.model.api.web.controller.request.HeadcountDistributionRequest;
import com.mercadolibre.planning.model.api.web.controller.request.HeadcountProductivityDataRequest;
import com.mercadolibre.planning.model.api.web.controller.request.HeadcountProductivityRequest;
import com.mercadolibre.planning.model.api.web.controller.request.MetadataRequest;
import com.mercadolibre.planning.model.api.web.controller.request.PlanningDistributionRequest;
import com.mercadolibre.planning.model.api.web.controller.request.PolyvalentProductivityRequest;
import com.mercadolibre.planning.model.api.web.controller.request.ProcessingDistributionDataRequest;
import com.mercadolibre.planning.model.api.web.controller.request.ProcessingDistributionRequest;
import com.mercadolibre.planning.model.api.web.controller.request.Source;
import com.mercadolibre.planning.model.api.web.controller.simulation.Simulation;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.PERCENTAGE;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PUT_TO_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.ACTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.PERFORMED_PROCESSING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.REMAINING_PROCESSING;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.FORECAST;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.SIMULATION;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;

@SuppressWarnings({"PMD.ExcessiveImports", "PMD.CouplingBetweenObjects"})
public final class TestUtils {

    public static final ZonedDateTime A_DATE_UTC = ZonedDateTime.of(2020, 8, 19, 17, 0, 0, 0,
            ZoneId.of("UTC"));
    public static final ZonedDateTime DATE_IN = ZonedDateTime.of(2020, 8, 19, 18, 0, 0, 0,
            ZoneId.of("UTC"));
    public static final ZonedDateTime DATE_OUT = ZonedDateTime.of(2020, 8, 20, 15, 30, 0, 0,
            ZoneId.of("UTC"));

    public static final String FORECAST_METADATA_KEY = "mono_order_distribution";
    public static final String FORECAST_METADATA_VALUE = "48";
    public static final String PLANNING_METADATA_KEY = "carrier";
    public static final String PLANNING_METADATA_VALUE = "Mercado envíos";
    public static final String WAREHOUSE_ID = "ARBA01";
    public static final long HOUR_IN_MINUTES = 60L;
    public static final String LOGISTIC_CENTER_ID = "ARBA01";
    public static final String CONFIG_KEY = "expedition_processing_time";
    public static final long USER_ID = 1234L;

    public static Forecast mockForecast(final Set<HeadcountDistribution> headcountDists,
                                        final Set<HeadcountProductivity> productivities,
                                        final Set<PlanningDistribution> planningDists,
                                        final Set<ProcessingDistribution> procDists,
                                        final Set<ForecastMetadata> forecastMetadatas) {
        return forecastEntityBuilder()
                .headcountDistributions(headcountDists)
                .processingDistributions(procDists)
                .headcountProductivities(productivities)
                .planningDistributions(planningDists)
                .metadatas(forecastMetadatas)
                .build();
    }

    public static Forecast mockSimpleForecast() {
        return forecastEntityBuilder().build();
    }

    public static Forecast.ForecastBuilder forecastEntityBuilder() {
        return Forecast.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .dateCreated(A_DATE_UTC)
                .lastUpdated(A_DATE_UTC);
    }

    public static PlanningDistribution mockPlanningDist(final Forecast forecast) {
        return planningBuilder().forecast(forecast).build();
    }

    public static PlanningDistribution.PlanningDistributionBuilder planningBuilder() {
        return PlanningDistribution.builder()
                .dateIn(DATE_IN)
                .dateOut(DATE_OUT)
                .quantity(1200)
                .metadatas(emptySet())
                .quantityMetricUnit(UNITS);
    }

    public static HeadcountDistribution.HeadcountDistributionBuilder headcountDistBuilder() {
        return HeadcountDistribution.builder()
                .area("MZ")
                .processName(PICKING)
                .quantity(40)
                .quantityMetricUnit(WORKERS);
    }

    public static HeadcountDistribution mockHeadcountDist(final Forecast forecast) {

        return headcountDistBuilder().forecast(forecast).build();
    }

    public static HeadcountProductivity.HeadcountProductivityBuilder headcountProdBuilder() {
        return HeadcountProductivity.builder()
                .abilityLevel(1)
                .productivity(80)
                .productivityMetricUnit(PERCENTAGE)
                .processName(PACKING)
                .date(A_DATE_UTC);
    }

    public static HeadcountProductivity mockHeadcountProd(final Forecast forecast) {
        return headcountProdBuilder().forecast(forecast).build();
    }

    public static ProcessingDistribution.ProcessingDistBuilder processDistBuilder() {
        return ProcessingDistribution.builder()
                .processName(WAVING)
                .quantity(1000)
                .quantityMetricUnit(UNITS)
                .type(REMAINING_PROCESSING)
                .date(A_DATE_UTC);
    }

    public static ProcessingDistribution mockProcessingDist(final Forecast forecast) {
        return processDistBuilder().forecast(forecast).build();
    }

    public static ForecastMetadata.ForecastMetadataBuilder forecastMetadataBuilder() {
        return ForecastMetadata.builder()
                .key(FORECAST_METADATA_KEY)
                .value(FORECAST_METADATA_VALUE);
    }

    public static ForecastMetadata mockForecastMetadata(final Forecast forecast) {
        if (forecast == null) {
            return forecastMetadataBuilder().build();
        }

        return forecastMetadataBuilder().forecastId(forecast.getId()).build();
    }

    public static PlanningDistributionMetadata
            .PlanningDistributionMetadataBuilder planningMetadataBuilder() {

        return PlanningDistributionMetadata.builder()
                .key(PLANNING_METADATA_KEY)
                .value(PLANNING_METADATA_VALUE);
    }

    public static PlanningDistributionMetadata mockPlanningDistMetadata(
            final PlanningDistribution planningDistEntity) {

        if (planningDistEntity == null) {
            return planningMetadataBuilder().build();
        }

        return planningMetadataBuilder()
                .planningDistributionId(planningDistEntity.getId())
                .build();
    }

    public static CurrentHeadcountProductivity mockCurrentProdEntity(final ZonedDateTime date,
                                                                     final long value) {
        return CurrentHeadcountProductivity.builder()
                .abilityLevel(1)
                .date(date)
                .isActive(true)
                .productivity(value)
                .productivityMetricUnit(UNITS_PER_HOUR)
                .processName(PICKING)
                .logisticCenterId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .logisticCenterId(WAREHOUSE_ID)
                .build();
    }

    public static CurrentProcessingDistribution mockCurrentProcDist(final ZonedDateTime date,
                                                                    final long value) {
        return CurrentProcessingDistribution.builder()
                .date(date)
                .isActive(true)
                .processName(PACKING)
                .quantity(value)
                .quantityMetricUnit(WORKERS)
                .type(ACTIVE_WORKERS)
                .workflow(FBM_WMS_OUTBOUND)
                .logisticCenterId(WAREHOUSE_ID)
                .build();
    }

    public static List<PlanningDistributionView> planningDistributions() {
        return List.of(
                new PlanningDistributionViewImpl(
                        Date.from(A_DATE_UTC.toInstant()),
                        Date.from(A_DATE_UTC.plusDays(1).toInstant()),
                        1000,
                        UNITS),
                new PlanningDistributionViewImpl(
                        Date.from(A_DATE_UTC.toInstant()),
                        Date.from(A_DATE_UTC.plusDays(2).toInstant()),
                        1200,
                        UNITS)
        );
    }

    public static CreateForecastInput mockCreateForecastInput() {
        return CreateForecastInput.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .headcountDistributions(mockHeadcounts())
                .headcountProductivities(mockProductivities())
                .planningDistributions(mockPlanningDistributions())
                .processingDistributions(mockProcessingDistributions())
                .polyvalentProductivities(mockPolyvalentProductivities())
                .metadata(mockMetadatas())
                .build();
    }

    public static GetHeadcountInput mockGetHeadcountEntityInput(final Source source) {
        return mockGetHeadcountEntityInput(source, null, null);
    }


    public static GetHeadcountInput mockGetHeadcountEntityInput(
            final Source source,
            final Set<ProcessingType> processingTypes,
            final List<Simulation> simulations) {
        return GetHeadcountInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .entityType(HEADCOUNT)
                .dateFrom(A_DATE_UTC)
                .dateTo(A_DATE_UTC.plusDays(2))
                .source(source)
                .processName(List.of(PICKING, PACKING))
                .processingType(processingTypes)
                .simulations(simulations)
                .build();
    }

    public static GetProductivityInput mockGetProductivityEntityInput(
            final Source source,
            final List<Simulation> simulations) {
        return GetProductivityInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .entityType(HEADCOUNT)
                .dateFrom(A_DATE_UTC)
                .dateTo(A_DATE_UTC.plusDays(2))
                .source(source)
                .processName(List.of(PICKING, PACKING))
                .simulations(simulations)
                .abilityLevel(Set.of(1))
                .build();
    }

    public static GetSuggestedWavesInput mockGetSuggestedWavesInput(final ZonedDateTime now) {
        return GetSuggestedWavesInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .dateFrom(now.plusHours(1).truncatedTo(HOURS).withFixedOffsetZone())
                .backlog(500)
                .dateTo(now.plusHours(2).truncatedTo(HOURS).withFixedOffsetZone())
                .build();
    }

    public static GetEntityInput mockGetThroughputEntityInput(final Source source,
                                                              final List<Simulation> simulations) {
        return GetEntityInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .entityType(HEADCOUNT)
                .dateFrom(A_DATE_UTC)
                .dateTo(A_DATE_UTC.plusDays(1))
                .source(source)
                .processName(List.of(PICKING, PACKING))
                .simulations(simulations)
                .build();
    }

    public static GetPlanningDistributionInput mockPlanningDistributionInput(
            final ZonedDateTime dateInTo) {
        return GetPlanningDistributionInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .dateOutFrom(A_DATE_UTC)
                .dateOutTo(A_DATE_UTC.plusDays(3))
                .dateInTo(dateInTo)
                .build();
    }

    public static GetForecastMetadataInput mockForecastMetadataInput() {
        return GetForecastMetadataInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .dateFrom(DATE_IN)
                .dateTo(DATE_OUT)
                .build();
    }

    public static List<ForecastMetadataView> mockForecastByWarehouseId() {
        return List.of(
                new ForecastMetadataViewImpl(
                        "mono_order_distribution",
                        "20"),
                new ForecastMetadataViewImpl(
                        "multi_order_distribution",
                        "20"),
                new ForecastMetadataViewImpl(
                        "multi_batch_distribution",
                        "60")
        );
    }

    public static List<EntityOutput> mockHeadcountEntityOutput() {
        return List.of(
                EntityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC)
                        .processName(PICKING)
                        .metricUnit(WORKERS)
                        .source(FORECAST)
                        .value(50)
                        .build(),
                EntityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC.plusHours(1))
                        .processName(PICKING)
                        .metricUnit(WORKERS)
                        .source(FORECAST)
                        .value(40)
                        .build(),
                EntityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC)
                        .processName(PACKING)
                        .metricUnit(WORKERS)
                        .source(FORECAST)
                        .value(60)
                        .build(),
                EntityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC.plusHours(1))
                        .processName(PACKING)
                        .metricUnit(WORKERS)
                        .source(FORECAST)
                        .value(30)
                        .build()
        );
    }

    public static List<EntityOutput> mockHeadcountEntityOutputWhitSimulations() {
        return List.of(
                EntityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC)
                        .processName(PICKING)
                        .metricUnit(WORKERS)
                        .source(FORECAST)
                        .value(50)
                        .build(),
                EntityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC.plusHours(1))
                        .processName(PICKING)
                        .metricUnit(WORKERS)
                        .source(FORECAST)
                        .value(40)
                        .build(),
                EntityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC)
                        .processName(PICKING)
                        .metricUnit(WORKERS)
                        .source(SIMULATION)
                        .value(60)
                        .build(),
                EntityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC.plusHours(1))
                        .processName(PICKING)
                        .metricUnit(WORKERS)
                        .source(SIMULATION)
                        .value(30)
                        .build(),
                EntityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC)
                        .processName(PACKING)
                        .metricUnit(WORKERS)
                        .source(FORECAST)
                        .value(60)
                        .build(),
                EntityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC.plusHours(1))
                        .processName(PACKING)
                        .metricUnit(WORKERS)
                        .source(FORECAST)
                        .value(30)
                        .build(),
                EntityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC)
                        .processName(PACKING)
                        .metricUnit(WORKERS)
                        .source(SIMULATION)
                        .value(70)
                        .build(),
                EntityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC.plusHours(1))
                        .processName(PACKING)
                        .metricUnit(WORKERS)
                        .source(SIMULATION)
                        .value(20)
                        .build()
        );
    }

    public static List<ProductivityOutput> mockProductivityEntityOutput() {
        return List.of(
                ProductivityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC)
                        .processName(PICKING)
                        .metricUnit(UNITS_PER_HOUR)
                        .source(FORECAST)
                        .value(80)
                        .abilityLevel(1)
                        .build(),
                ProductivityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC.plusHours(1))
                        .processName(PICKING)
                        .metricUnit(UNITS_PER_HOUR)
                        .source(FORECAST)
                        .value(70)
                        .abilityLevel(1)
                        .build(),
                ProductivityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC)
                        .processName(PACKING)
                        .metricUnit(UNITS_PER_HOUR)
                        .source(FORECAST)
                        .value(85)
                        .abilityLevel(1)
                        .build(),
                ProductivityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC.plusHours(1))
                        .processName(PACKING)
                        .metricUnit(UNITS_PER_HOUR)
                        .source(FORECAST)
                        .value(90)
                        .abilityLevel(1)
                        .build()
        );
    }

    public static List<ProductivityOutput> mockProductivityEntityOutputWithSimulations() {
        return List.of(
                ProductivityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC)
                        .processName(PICKING)
                        .metricUnit(UNITS_PER_HOUR)
                        .source(FORECAST)
                        .value(80)
                        .abilityLevel(1)
                        .build(),
                ProductivityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC.plusHours(1))
                        .processName(PICKING)
                        .metricUnit(UNITS_PER_HOUR)
                        .source(FORECAST)
                        .value(70)
                        .abilityLevel(1)
                        .build(),
                ProductivityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC)
                        .processName(PICKING)
                        .metricUnit(UNITS_PER_HOUR)
                        .source(SIMULATION)
                        .value(90)
                        .abilityLevel(1)
                        .build(),
                ProductivityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC.plusHours(1))
                        .processName(PICKING)
                        .metricUnit(UNITS_PER_HOUR)
                        .source(SIMULATION)
                        .value(50)
                        .abilityLevel(1)
                        .build(),
                ProductivityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC)
                        .processName(PACKING)
                        .metricUnit(UNITS_PER_HOUR)
                        .source(FORECAST)
                        .value(85)
                        .abilityLevel(1)
                        .build(),
                ProductivityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC.plusHours(1))
                        .processName(PACKING)
                        .metricUnit(UNITS_PER_HOUR)
                        .source(FORECAST)
                        .value(90)
                        .abilityLevel(1)
                        .build(),
                ProductivityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC)
                        .processName(PACKING)
                        .metricUnit(UNITS_PER_HOUR)
                        .source(SIMULATION)
                        .value(75)
                        .abilityLevel(1)
                        .build(),
                ProductivityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC.plusHours(1))
                        .processName(PACKING)
                        .metricUnit(UNITS_PER_HOUR)
                        .source(SIMULATION)
                        .value(100)
                        .abilityLevel(1)
                        .build()
        );
    }

    public static List<ProductivityOutput> mockMultiFunctionalProductivityEntityOutput() {
        return List.of(
                ProductivityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC)
                        .processName(PICKING)
                        .metricUnit(UNITS_PER_HOUR)
                        .source(FORECAST)
                        .value(40)
                        .abilityLevel(2)
                        .build(),
                ProductivityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC.plusHours(1))
                        .processName(PICKING)
                        .metricUnit(UNITS_PER_HOUR)
                        .source(FORECAST)
                        .value(35)
                        .abilityLevel(2)
                        .build(),
                ProductivityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC)
                        .processName(PACKING)
                        .metricUnit(UNITS_PER_HOUR)
                        .source(FORECAST)
                        .value(40)
                        .abilityLevel(2)
                        .build(),
                ProductivityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC.plusHours(1))
                        .processName(PACKING)
                        .metricUnit(UNITS_PER_HOUR)
                        .source(FORECAST)
                        .value(45)
                        .abilityLevel(2)
                        .build()
        );
    }

    public static List<EntityOutput> mockThroughputEntityOutput() {
        return List.of(
                EntityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC)
                        .processName(PICKING)
                        .metricUnit(UNITS_PER_HOUR)
                        .source(FORECAST)
                        .value(800)
                        .build(),
                EntityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC.plusHours(1))
                        .processName(PICKING)
                        .metricUnit(UNITS_PER_HOUR)
                        .source(FORECAST)
                        .value(600)
                        .build(),
                EntityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC)
                        .processName(PACKING)
                        .metricUnit(UNITS_PER_HOUR)
                        .source(FORECAST)
                        .value(700)
                        .build(),
                EntityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC.plusHours(1))
                        .processName(PACKING)
                        .metricUnit(UNITS_PER_HOUR)
                        .source(FORECAST)
                        .value(550)
                        .build()
        );
    }

    public static List<GetPlanningDistributionOutput> mockGetPlanningDistOutput() {
        return List.of(
                GetPlanningDistributionOutput.builder()
                        .dateIn(A_DATE_UTC)
                        .dateOut(A_DATE_UTC.plusDays(1))
                        .metricUnit(UNITS)
                        .total(1500)
                        .build(),
                GetPlanningDistributionOutput.builder()
                        .dateIn(A_DATE_UTC)
                        .dateOut(A_DATE_UTC.plusDays(2))
                        .metricUnit(UNITS)
                        .total(1800)
                        .build(),
                GetPlanningDistributionOutput.builder()
                        .dateIn(A_DATE_UTC)
                        .dateOut(A_DATE_UTC.plusDays(3))
                        .metricUnit(UNITS)
                        .total(1700)
                        .build()
        );
    }

    public static String getResourceAsString(final String resourceName) throws IOException {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final InputStream resource = classLoader.getResourceAsStream(resourceName);

        try {
            return IOUtils.toString(resource, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            resource.close();
        }
    }

    private static List<ProcessingDistributionRequest> mockProcessingDistributions() {
        final List<ProcessingDistributionDataRequest> data = asList(
                new ProcessingDistributionDataRequest(DATE_IN, 172),
                new ProcessingDistributionDataRequest(DATE_IN.plusHours(1), 295)
        );

        return singletonList(
                new ProcessingDistributionRequest(PERFORMED_PROCESSING, UNITS, WAVING, data)
        );
    }

    private static List<PolyvalentProductivityRequest> mockPolyvalentProductivities() {
        return asList(
                new PolyvalentProductivityRequest(PACKING, PERCENTAGE, 90, 1),
                new PolyvalentProductivityRequest(PICKING, PERCENTAGE, 86, 1)
        );
    }

    private static List<PlanningDistributionRequest> mockPlanningDistributions() {
        return singletonList(
                new PlanningDistributionRequest(
                        DATE_IN, DATE_OUT, UNITS, 1200, asList(
                        new MetadataRequest("carrier_id", "17502740"),
                        new MetadataRequest("service_id", "851"),
                        new MetadataRequest("canalization", "U")
                ))
        );
    }

    private static List<MetadataRequest> mockMetadatas() {
        return asList(
                new MetadataRequest("warehouse_id", WAREHOUSE_ID),
                new MetadataRequest("week", "26-2020"),
                new MetadataRequest("mono_order_distribution", "58"),
                new MetadataRequest("multi_order_distribution", "42")
        );
    }

    private static List<HeadcountProductivityRequest> mockProductivities() {
        return asList(
                new HeadcountProductivityRequest(PICKING, UNITS_PER_HOUR, 0, List.of(
                        new HeadcountProductivityDataRequest(A_DATE_UTC, 85),
                        new HeadcountProductivityDataRequest(A_DATE_UTC.plusHours(1), 85)
                )),
                new HeadcountProductivityRequest(PACKING, UNITS_PER_HOUR, 0, List.of(
                        new HeadcountProductivityDataRequest(A_DATE_UTC, 92),
                        new HeadcountProductivityDataRequest(A_DATE_UTC.plusHours(1), 85)
                ))
        );
    }

    private static List<HeadcountDistributionRequest> mockHeadcounts() {
        return asList(
                new HeadcountDistributionRequest(PICKING, PERCENTAGE, mockPickingAreas()),
                new HeadcountDistributionRequest(PUT_TO_WALL, PERCENTAGE, mockPtwAreas())
        );
    }

    private static List<AreaRequest> mockPickingAreas() {
        return asList(
                new AreaRequest("MZ", 85),
                new AreaRequest("RS", 5),
                new AreaRequest("HV", 5),
                new AreaRequest("BL", 5)
        );
    }

    private static List<AreaRequest> mockPtwAreas() {
        return asList(
                new AreaRequest("IN", 60),
                new AreaRequest("OUT", 40)
        );
    }
    

    
    public static List<EntityOutput> mockGetRemainingProcessingOutput() {
        return List.of(
                EntityOutput.builder()
                        .date(A_DATE_UTC)
                        .metricUnit(UNITS)
                        .processName(WAVING)
                        .source(FORECAST)
                        .type(REMAINING_PROCESSING)
                        .value(1500)
                        .workflow(FBM_WMS_OUTBOUND)
                        .build(),
                        EntityOutput.builder()
                        .date(A_DATE_UTC)
                        .metricUnit(UNITS)
                        .processName(WAVING)
                        .source(FORECAST)
                        .type(REMAINING_PROCESSING)
                        .value(1500)
                        .workflow(FBM_WMS_OUTBOUND)
                        .build()
        );
    }
}
