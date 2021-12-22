package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.BacklogProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.CalculateBacklogProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.BacklogProjection;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.SIMULATION;

@Component
@AllArgsConstructor
public class GetOutboundBacklogProjectionUseCase implements GetBacklogProjectionUseCase {

    private final CalculateBacklogProjectionUseCase calculateBacklogProjection;

    private final GetThroughputUseCase getThroughputUseCase;

    private final GetPlanningDistributionUseCase getPlanningUseCase;

    @Override
    public List<BacklogProjection> execute(final BacklogProjectionInput input) {

        final List<EntityOutput> throughput = getThroughputUseCase.execute(GetEntityInput
                .builder()
                .warehouseId(input.getLogisticCenterId())
                .dateFrom(input.getDateFrom().minusHours(1))
                .dateTo(input.getDateTo())
                .source(SIMULATION)
                .processName(input.getProcessNames())
                .workflow(getWorkflow())
                .build());

        final List<GetPlanningDistributionOutput> planningUnits = getPlanningUseCase.execute(
                GetPlanningDistributionInput.builder()
                        .workflow(getWorkflow())
                        .warehouseId(input.getLogisticCenterId())
                        .dateInTo(input.getDateTo().plusDays(1))
                        .dateOutFrom(input.getDateFrom())
                        .dateOutTo(input.getDateFrom().plusDays(1))
                        .applyDeviation(true)
                        .build());

        return calculateBacklogProjection.execute(BacklogProjectionInput.builder()
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .throughputs(throughput)
                .currentBacklogs(input.getCurrentBacklogs())
                .processNames(input.getProcessNames())
                .planningUnits(planningUnits)
                .build());
    }

    @Override
    public Workflow getWorkflow() {
        return Workflow.FBM_WMS_OUTBOUND;
    }
}
