package com.mercadolibre.planning.model.api.domain.usecase.projection.capacity;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseInput;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseOutput;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedBacklogService;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedUnits;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.CapacityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.GetCapacityPerHourService;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.Backlog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CalculateCptProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptCalculationOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.SlaProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetSlaProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.sla.GetSlaByWarehouseInboundService;
import com.mercadolibre.planning.model.api.domain.usecase.sla.GetSlaByWarehouseOutboundService;
import com.mercadolibre.planning.model.api.web.controller.projection.request.QuantityByDate;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.mercadolibre.planning.model.api.domain.usecase.capacity.CapacityInput.fromEntityOutputs;
import static com.mercadolibre.planning.model.api.util.DateUtils.getCurrentUtcDate;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Component
@AllArgsConstructor
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.LongVariable"})
public class GetSlaProjectionUseCase {

    private final CalculateCptProjectionUseCase calculateCptProjection;

    private final GetThroughputUseCase getThroughputUseCase;

    private final GetCapacityPerHourService getCapacityPerHourService;

    private final GetSlaByWarehouseOutboundService getSlaByWarehouseOutboundService;

    private final GetSlaByWarehouseInboundService getSlaByWarehouseInboundService;

    private final PlannedBacklogService plannedBacklogService;

    public List<CptProjectionOutput> execute(final GetSlaProjectionInput request) {
        final Workflow workflow = request.getWorkflow();
        final String warehouseId = request.getWarehouseId();
        final ZonedDateTime dateFrom = request.getDateFrom();
        final ZonedDateTime dateTo = request.getDateTo();
        final String timeZone = request.getTimeZone();

        final List<EntityOutput> throughput = getThroughputUseCase.execute(GetEntityInput
                .builder()
                .warehouseId(warehouseId)
                .workflow(workflow)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .processName(request.getProcessName())
                .simulations(request.getSimulations())
                .source(request.getSource())
                .build());

        final Map<ZonedDateTime, Integer> capacity = getCapacityPerHourService
                .execute(workflow, fromEntityOutputs(throughput))
                .stream()
                .collect(toMap(
                        CapacityOutput::getDate,
                        capacityOutput -> (int) capacityOutput.getValue()
                ));

        final List<PlannedUnits> expectedBacklog = plannedBacklogService.getExpectedBacklog(
                request.getWarehouseId(),
                workflow,
                request.getDateFrom(),
                request.getDateTo(),
                request.isApplyDeviation()
        );

        final List<GetSlaByWarehouseOutput> slaByWarehouse = slaByWarehouseAndWorkflow(
                workflow,
                warehouseId,
                dateFrom,
                dateTo,
                request.getBacklog(),
                expectedBacklog,
                timeZone);

        final List<CptCalculationOutput> cptProjectionOutputs =
                calculateCptProjection.execute(SlaProjectionInput.builder()
                        .workflow(workflow)
                        .logisticCenterId(warehouseId)
                        .dateFrom(dateFrom)
                        .dateTo(dateTo)
                        .capacity(capacity)
                        .backlog(getBacklog(request.getBacklog()))
                        .plannedUnits(expectedBacklog)
                        .slaByWarehouse(slaByWarehouse)
                        .currentDate(getCurrentUtcDate())
                        .build());

        return cptProjectionOutputs.stream()
                .map(item -> new CptProjectionOutput(
                        item.getDate(),
                        item.getProjectedEndDate(),
                        item.getRemainingQuantity())
                ).collect(toList());
    }

    private List<Backlog> getBacklog(final List<QuantityByDate> backlogs) {
        return backlogs == null
                ? emptyList()
                : backlogs.stream().map(QuantityByDate::toBacklog).collect(toList());
    }

    private List<ZonedDateTime> getCptDefaultFromBacklog(final List<QuantityByDate> backlogs) {
        return backlogs == null
                ? emptyList()
                : backlogs.stream().map(QuantityByDate::getDate).distinct().collect(toList());
    }

    private List<ZonedDateTime> getDefaultSlasFromExpectedUnits(final List<PlannedUnits> plannedUnits) {
        return plannedUnits == null
                ? emptyList()
                : plannedUnits.stream()
                    .map(PlannedUnits::getDateOut)
                    .map(date -> date.withZoneSameInstant(ZoneId.of("UTC")))
                    .distinct()
                    .collect(toList());
    }

    private List<GetSlaByWarehouseOutput> slaByWarehouseAndWorkflow(
            final Workflow workflow,
            final String warehouseId,
            final ZonedDateTime dateFrom,
            final ZonedDateTime dateTo,
            final List<QuantityByDate> backlog,
            final List<PlannedUnits> plannedUnits,
            final String timeZone) {

        if (workflow == Workflow.FBM_WMS_OUTBOUND) {
            final GetSlaByWarehouseInput getSlaByWarehouseInput = new GetSlaByWarehouseInput(
                    warehouseId, dateFrom, dateTo, getCptDefaultFromBacklog(backlog), timeZone
            );


            return getSlaByWarehouseOutboundService.execute(getSlaByWarehouseInput);
        } else if (workflow == Workflow.FBM_WMS_INBOUND) {
            final List<ZonedDateTime> defaults = Stream.concat(
                            getCptDefaultFromBacklog(backlog).stream(),
                            getDefaultSlasFromExpectedUnits(plannedUnits).stream()
                    )
                    .distinct()
                    .collect(toList());

            final GetSlaByWarehouseInput getSlaByWarehouseInput = new GetSlaByWarehouseInput(
                    warehouseId, dateFrom, dateTo, defaults, timeZone
            );

            return getSlaByWarehouseInboundService.execute(getSlaByWarehouseInput);
        }
        return emptyList();
    }
}
