package com.mercadolibre.planning.model.api.domain.usecase.suggestedwave.get;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ForecastMetadataView;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.SuggestedWavePlanningDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.WaveCardinality;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.remainingprocessing.get.GetRemainingProcessingUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastMetadataInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastMetadataUseCase;
import com.newrelic.api.agent.Trace;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.util.DateUtils.getForecastWeeks;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.REMAINING_PROCESSING;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.stream.Collectors.toList;

@Service
@AllArgsConstructor
public class GetSuggestedWavesUseCase
        implements UseCase<GetSuggestedWavesInput, List<SuggestedWavesOutput>> {

    private final GetRemainingProcessingUseCase getRemainingProcessingUseCase;
    private final GetForecastMetadataUseCase getForecastMetadataUseCase;

    private final PlanningDistributionRepository planningDistRepository;

    private static final long HOUR_IN_MINUTES = 60L;

    @Trace
    @Override
    public List<SuggestedWavesOutput> execute(final GetSuggestedWavesInput input) {
        final Set<String> forecastWeeks = getForecastWeeks(input.getDateFrom(), input.getDateTo());

        final long sales = getBoundedSales(input,
                forecastWeeks,
                ZonedDateTime.now(Clock.systemUTC()));

        final long remainingProcessing = getRemainingProcessing(input);

        final long unitsToWave = Math.max(input.getBacklog() + sales - remainingProcessing, 0);

        final List<ForecastMetadataView> forecastMetadataPercentage = getForecastMetadataUseCase
                .execute(GetForecastMetadataInput.builder()
                        .workflow(input.getWorkflow())
                        .warehouseId(input.getWarehouseId())
                        .dateFrom(input.getDateFrom())
                        .dateTo(input.getDateTo())
                        .build()
                );

        return forecastMetadataPercentage.stream()
                .map(fm -> new SuggestedWavesOutput(
                        WaveCardinality.of(fm.getKey()).orElse(null),
                        calculateSuggestedWave(unitsToWave,Float.parseFloat(fm.getValue()))))
                .collect(toList());
    }

    private long calculateSuggestedWave(final  Long waveSuggest, final float percentage) {
        return (long) Math.floor((percentage / 100) * waveSuggest);
    }

    private long getRemainingProcessing(final GetSuggestedWavesInput input) {
        return getRemainingProcessingUseCase.execute(GetEntityInput.builder()
                .workflow(input.getWorkflow())
                .processName(List.of(PICKING))
                .warehouseId(input.getWarehouseId())
                .entityType(REMAINING_PROCESSING)
                .dateFrom(input.getDateTo().minusHours(1))
                .dateTo(input.getDateTo().minusHours(1))
                .build()
        ).stream().findFirst()
                .map(EntityOutput::getValue)
                .orElse(0L);
    }

    private long getBoundedSales(final GetSuggestedWavesInput input,
                              final Set<String> forecastWeeks,
                              final ZonedDateTime now) {

        final SuggestedWavePlanningDistributionView currentHourSales = planningDistRepository
                .findByWarehouseIdWorkflowDateInRange(
                        input.getWarehouseId(),
                        input.getWorkflow().name(),
                        now.truncatedTo(HOURS),
                        now.truncatedTo(HOURS).plusHours(1).minusMinutes(1),
                        forecastWeeks);
        final SuggestedWavePlanningDistributionView nextHourSales = planningDistRepository
                .findByWarehouseIdWorkflowDateInRange(
                        input.getWarehouseId(),
                        input.getWorkflow().name(),
                        now.plusHours(1).truncatedTo(HOURS),
                        input.getDateTo().minusMinutes(1),
                        forecastWeeks);

        return (currentHourSales == null ? 0 : currentHourSalesPercentage(
                currentHourSales.getQuantity(), now))
                + (nextHourSales == null ? 0 : nextHourSales.getQuantity());
    }

    private long currentHourSalesPercentage(final long currentHourSales,
                                            final ZonedDateTime now) {
        return  currentHourSales * (HOUR_IN_MINUTES - now.withFixedOffsetZone().getMinute())
                / HOUR_IN_MINUTES;
    }
}
