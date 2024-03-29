package com.mercadolibre.planning.model.api.web.controller.plan.staffing;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.CHECK_IN;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.SIMULATION;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpStatus.OK;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.service.lastupdatedentity.LastEntityModifiedDateService;
import com.mercadolibre.planning.model.api.domain.service.lastupdatedentity.LastModifiedDates;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.ProductivityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.activate.ActivateSimulationUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.activate.SimulationInput;
import com.mercadolibre.planning.model.api.util.StaffingPlanMapper;
import com.mercadolibre.planning.model.api.web.controller.editor.EntityTypeEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.ProcessNameEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.ProcessPathEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import com.mercadolibre.planning.model.api.web.controller.plan.staffing.request.StaffingPlanUpdateRequest;
import com.mercadolibre.planning.model.api.web.controller.plan.staffing.request.UpdateStaffingPlanRequest;
import com.newrelic.api.agent.Trace;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/logistic_center/{logisticCenterId}/plan/staffing")
public class Controller {

  private static final Map<Workflow, List<ProcessName>> PROCESSES_BY_WORKFLOW = Map.of(
      Workflow.FBM_WMS_OUTBOUND, List.of(PICKING, PACKING, PACKING_WALL, BATCH_SORTER, WALL_IN),
      Workflow.FBM_WMS_INBOUND, List.of(CHECK_IN, PUT_AWAY)
  );

  private static final String STAFFING_PLAN = "staffing_plan";

  private final GetThroughputUseCase getThroughputUseCase;

  private final GetHeadcountEntityUseCase getHeadcountEntityUseCase;

  private final GetProductivityEntityUseCase getProductivityEntityUseCase;

  private final ActivateSimulationUseCase activateSimulationUseCase;

  private final LastEntityModifiedDateService lastEntityModifiedService;

  @GetMapping
  @Trace(dispatcher = true)
  public ResponseEntity<StaffingPlanMapper.StaffingPlan> getStaffingPlan(
      @PathVariable final String logisticCenterId,
      @RequestParam final Workflow workflow,
      @RequestParam(required = false) final List<ProcessPath> processPaths,
      @RequestParam(required = false) final Set<ProcessName> processes,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Instant dateFrom,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Instant dateTo,
      @RequestParam final Instant viewDate
  ) {

    final GetEntityInput input = createEntityInput(
        logisticCenterId,
        workflow,
        processPaths,
        processes,
        dateFrom,
        dateTo,
        viewDate
    );
    final List<EntityOutput> headcounts = getHeadcountEntityUseCase.execute(StaffingPlanMapper.createSystemicHeadcountInput(input));
    final List<EntityOutput> headcountsNs = getHeadcountEntityUseCase.execute(StaffingPlanMapper.createNonSystemicHeadcountInput(input));
    final List<ProductivityOutput> productivity = getProductivityEntityUseCase.execute(StaffingPlanMapper.createProductivityInput(input));
    final List<EntityOutput> throughput = getThroughputUseCase.execute(input);

    return ResponseEntity.status(OK).body(new StaffingPlanMapper.StaffingPlan(
        StaffingPlanMapper.adaptEntityOutputResponse(headcounts),
        StaffingPlanMapper.adaptEntityOutputResponse(headcountsNs),
        StaffingPlanMapper.adaptEntityOutputResponse(
            productivity.stream().filter(ProductivityOutput::isMainProductivity).collect(toList())
        ),
        StaffingPlanMapper.adaptThroughputResponse(throughput)
    ));
  }

  /**
   * Update staffing plan.
   * @param logisticCenterId logistic center id
   * @param workflow workflow
   * @param userId user id
   * @param request request
   * @deprecated use {@link StaffingPlanController#updateStaffingPlan(String, Workflow, long, StaffingPlanUpdateRequest)}
   */
  @ResponseStatus(OK)
  @PutMapping
  @Trace(dispatcher = true)
  @Deprecated
  public void updateStaffingPlan(@PathVariable final String logisticCenterId,
                                 @RequestParam final Workflow workflow,
                                 @RequestParam final long userId,
                                 @RequestBody final UpdateStaffingPlanRequest request) {

    final SimulationInput input = request.toSimulationInput(workflow, logisticCenterId, userId);
    activateSimulationUseCase.execute(input);
  }

  @GetMapping("/throughput")
  @Trace(dispatcher = true)
  public ResponseEntity<Map<ProcessPath, Map<ProcessName, Map<Instant, StaffingPlanMapper.StaffingPlanThroughput>>>> getThroughput(
      @PathVariable final String logisticCenterId,
      @RequestParam final Workflow workflow,
      @RequestParam(required = false) final List<ProcessPath> processPaths,
      @RequestParam(required = false) final Set<ProcessName> processes,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Instant dateFrom,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Instant dateTo,
      @RequestParam final Instant viewDate
  ) {
    final GetEntityInput input = createEntityInput(
        logisticCenterId,
        workflow,
        processPaths,
        processes,
        dateFrom,
        dateTo,
        viewDate
    );
    final var result = getThroughputUseCase.execute(input);
    return ResponseEntity.status(OK).body(StaffingPlanMapper.adaptThroughputResponse(result));
  }

  @GetMapping("last_updated")
  @Trace(dispatcher = true)
  public ResponseEntity<Map<String, Instant>> getLastModifiedDate(@PathVariable final String logisticCenterId,
                                                                  @RequestParam final Workflow workflow,
                                                                  @RequestParam(required = false) final Set<EntityType> entityType,
                                                                  @RequestParam final Instant viewDate
  ) {
    final LastModifiedDates lastModifiedDates =
        lastEntityModifiedService.getLastEntityDateModified(logisticCenterId, workflow, entityType, viewDate);

    return ResponseEntity.status(OK).body(toLastModifiedDatesResponse(lastModifiedDates));
  }

  private Map<String, Instant> toLastModifiedDatesResponse(final LastModifiedDates lastModifiedDates) {

    final Map<String, Instant> lastDates = new ConcurrentHashMap<>();
    lastDates.put(STAFFING_PLAN, lastModifiedDates.lastStaffingCreated());
    lastDates.putAll(lastModifiedDates.lastDateEntitiesUpdate().entrySet().stream().collect(
        Collectors.toMap(
            entry -> entry.getKey().toJson(),
            Map.Entry::getValue
        )
    ));

    return lastDates;
  }

  @InitBinder
  public void initBinder(final PropertyEditorRegistry dataBinder) {
    dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
    dataBinder.registerCustomEditor(ProcessPath.class, new ProcessPathEditor());
    dataBinder.registerCustomEditor(ProcessName.class, new ProcessNameEditor());
    dataBinder.registerCustomEditor(EntityType.class, new EntityTypeEditor());
  }

  private GetEntityInput createEntityInput(
      final String logisticCenterId,
      final Workflow workflow,
      final List<ProcessPath> processPaths,
      final Set<ProcessName> processes,
      final Instant dateFrom,
      final Instant dateTo,
      final Instant viewDate
  ) {

    // TODO retrieve all processes if processes params is empty based on processes stored in DB
    final var processesNames = CollectionUtils.isEmpty(processes)
        ? PROCESSES_BY_WORKFLOW.get(workflow)
        : new ArrayList<>(processes);

    return GetEntityInput.builder()
        .warehouseId(logisticCenterId)
        .workflow(workflow)
        .dateFrom(ZonedDateTime.ofInstant(dateFrom, ZoneOffset.UTC))
        .dateTo(ZonedDateTime.ofInstant(dateTo, ZoneOffset.UTC))
        .source(SIMULATION)
        .processName(processesNames)
        .processPaths(processPaths)
        .simulations(emptyList())
        .viewDate(viewDate)
        .build();
  }
}
