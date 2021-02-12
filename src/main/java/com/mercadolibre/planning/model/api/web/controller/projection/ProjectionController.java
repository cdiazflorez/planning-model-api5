package com.mercadolibre.planning.model.api.web.controller.projection;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.BacklogProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.CalculateBacklogProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.BacklogProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.Backlog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CalculateCptProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionOutput;
import com.mercadolibre.planning.model.api.web.controller.editor.ProjectionTypeEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.api.web.controller.projection.request.BacklogProjectionRequest;
import com.mercadolibre.planning.model.api.web.controller.projection.request.CptProjectionRequest;
import com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType;
import com.mercadolibre.planning.model.api.web.controller.projection.request.QuantityByDate;
import com.newrelic.api.agent.Trace;
import lombok.AllArgsConstructor;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.List;

import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.SIMULATION;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/workflows/{workflow}/projections")
@SuppressWarnings("PMD.ExcessiveImports")
public class ProjectionController {

    private final CalculateCptProjectionUseCase calculateCptProjection;
    private final CalculateBacklogProjectionUseCase calculateBacklogProjection;
    private final GetThroughputUseCase getThroughputUseCase;
    private final GetPlanningDistributionUseCase getPlanningUseCase;

    @PostMapping
    @Trace(dispatcher = true)
    public ResponseEntity<List<CptProjectionOutput>> getProjection(
            @PathVariable final Workflow workflow,
            @RequestBody final CptProjectionRequest request) {

        return generateCptProjection(workflow, request);
    }

    @PostMapping("/cpts")
    @Trace(dispatcher = true)
    public ResponseEntity<List<CptProjectionOutput>> getCptProjection(
            @PathVariable final Workflow workflow,
            @RequestBody final CptProjectionRequest request) {

        return generateCptProjection(workflow, request);
    }

    @PostMapping("/backlogs")
    @Trace(dispatcher = true)
    public ResponseEntity<List<BacklogProjectionOutput>> getBacklogProjections(
            @PathVariable final Workflow workflow,
            @RequestBody final BacklogProjectionRequest request) {

        final String warehouseId = request.getWarehouseId();
        final ZonedDateTime dateFrom = request.getDateFrom();
        final ZonedDateTime dateTo = request.getDateTo();

        final List<EntityOutput> throughput = getThroughputUseCase.execute(GetEntityInput
                .builder()
                .warehouseId(warehouseId)
                .dateFrom(dateFrom.minusHours(1))
                .dateTo(dateTo)
                .source(SIMULATION)
                .processName(request.getProcessName())
                .workflow(workflow)
                .build());

        final List<GetPlanningDistributionOutput> planningUnits = getPlanningUseCase.execute(
                GetPlanningDistributionInput.builder()
                        .workflow(workflow)
                        .warehouseId(request.getWarehouseId())
                        .dateInTo(request.getDateTo().plusDays(1))
                        .dateOutFrom(request.getDateFrom())
                        .dateOutTo(request.getDateTo().plusDays(1))
                        .applyDeviation(request.isApplyDeviation())
                        .build());

        return ResponseEntity
                .ok(calculateBacklogProjection.execute(BacklogProjectionInput.builder()
                        .dateFrom(dateFrom)
                        .dateTo(dateTo)
                        .throughputs(throughput)
                        .currentBacklogs(request.getCurrentBacklog())
                        .processNames(request.getProcessName())
                        .planningUnits(planningUnits)
                        .build()));
    }

    @InitBinder
    public void initBinder(final PropertyEditorRegistry dataBinder) {
        dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
        dataBinder.registerCustomEditor(ProjectionType.class, new ProjectionTypeEditor());
    }

    private ResponseEntity<List<CptProjectionOutput>> generateCptProjection(
            final Workflow workflow,
            final CptProjectionRequest request) {

        final String warehouseId = request.getWarehouseId();
        final ZonedDateTime dateFrom = request.getDateFrom();
        final ZonedDateTime dateTo = request.getDateTo();

        final List<EntityOutput> throughput = getThroughputUseCase.execute(GetEntityInput
                .builder()
                .warehouseId(warehouseId)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .processName(request.getProcessName())
                .workflow(workflow)
                .build());

        final List<GetPlanningDistributionOutput> planningUnits = getPlanningUseCase.execute(
                GetPlanningDistributionInput.builder()
                        .workflow(workflow)
                        .warehouseId(request.getWarehouseId())
                        .dateOutFrom(request.getDateFrom())
                        .dateOutTo(request.getDateTo())
                        .applyDeviation(request.isApplyDeviation())
                        .build());

        return ResponseEntity
                .ok(calculateCptProjection.execute(CptProjectionInput.builder()
                        .dateFrom(dateFrom)
                        .dateTo(dateTo)
                        .backlog(getBacklog(request.getBacklog()))
                        .throughput(throughput)
                        .planningUnits(planningUnits)
                        .build()));
    }

    private List<Backlog> getBacklog(final List<QuantityByDate> backlogs) {
        return backlogs == null
                ? emptyList()
                : backlogs.stream().map(QuantityByDate::toBacklog).collect(toList());
    }
}
