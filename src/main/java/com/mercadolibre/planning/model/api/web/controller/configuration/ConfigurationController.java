package com.mercadolibre.planning.model.api.web.controller.configuration;

import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static java.util.Collections.emptyMap;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.service.sla.OutboundSlaPropertiesService;
import com.mercadolibre.planning.model.api.domain.service.sla.SlaProperties;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.create.CreateConfigurationUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.update.UpdateConfigurationUseCase;
import com.mercadolibre.planning.model.api.web.controller.configuration.request.CreateConfigurationRequest;
import com.mercadolibre.planning.model.api.web.controller.configuration.request.CycleTimeRequest;
import com.mercadolibre.planning.model.api.web.controller.configuration.request.UpdateConfigurationRequest;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import com.newrelic.api.agent.Trace;
import java.time.Instant;
import java.util.Map;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Deprecated
@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/configuration")
public class ConfigurationController {

  private final CreateConfigurationUseCase createConfigurationUseCase;

  private final UpdateConfigurationUseCase updateConfigurationUseCase;

  private final OutboundSlaPropertiesService outboundSlaPropertiesService;

  @PostMapping
  @Trace(dispatcher = true)
  public ResponseEntity<ConfigurationResponse> create(
      @RequestBody @Valid final CreateConfigurationRequest request,
      @RequestParam(required = false, defaultValue = "0") final Long userId) {

    return ResponseEntity.ok(toResponse(
        createConfigurationUseCase.execute(request.toConfigurationInput(userId))
    ));
  }

  @PutMapping("/{logisticCenterId}/{key}")
  @Trace(dispatcher = true)
  public ResponseEntity<ConfigurationResponse> update(
      @PathVariable final String logisticCenterId,
      @PathVariable final String key,
      @RequestParam(required = false, defaultValue = "0") final Long userId,
      @RequestBody @Valid final UpdateConfigurationRequest request) {

    return ResponseEntity.ok(
        toResponse(
            updateConfigurationUseCase.execute(
                request.toConfigurationInput(logisticCenterId, key, userId))));
  }

  private ConfigurationResponse toResponse(final Configuration configuration) {

    try {
      return new ConfigurationResponse(
          Integer.parseInt(configuration.getValue()),
          configuration.getMetricUnit().toJson());
    } catch (NumberFormatException nfe) {
      log.warn("Could not parse configuration value {} to integer. Returning 0.", configuration.getValue());
      return new ConfigurationResponse(
          0,
          configuration.getMetricUnit().toJson());
    }
  }

  @PostMapping("/logistic_center/{logisticCenterId}/cycle_time/search")
  public ResponseEntity<Map<Workflow, Map<Instant, SlaProperties>>> search(
      @PathVariable final String logisticCenterId,
      @RequestBody @Valid final CycleTimeRequest request
  ) {

    if (request.workflows().contains(FBM_WMS_OUTBOUND)) {
      final var result = outboundSlaPropertiesService.get(
          new OutboundSlaPropertiesService.Input(
              logisticCenterId,
              FBM_WMS_OUTBOUND,
              request.slas()
          )
      );

      return ResponseEntity.ok()
          .body(Map.of(FBM_WMS_OUTBOUND, result));
    } else {
      return ResponseEntity.unprocessableEntity()
          .body(emptyMap());
    }
  }

  @InitBinder
  public void initBinder(final PropertyEditorRegistry dataBinder) {
    dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
  }
}
