package com.mercadolibre.planning.model.api.web.controller.entity.request;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import com.mercadolibre.planning.model.api.web.controller.projection.request.Source;
import com.mercadolibre.planning.model.api.web.controller.simulation.Simulation;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityRequest {

  @NotBlank
  protected String warehouseId;

  @NotNull
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  protected ZonedDateTime dateFrom;

  @NotNull
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  protected ZonedDateTime dateTo;

  protected Source source;

  @NotEmpty
  protected List<ProcessName> processName;

  protected List<Simulation> simulations;

  protected Instant viewDate;

  public GetEntityInput toGetEntityInput(final Workflow workflow) {
    return GetEntityInput.builder()
        .warehouseId(warehouseId)
        .workflow(workflow)
        .dateFrom(dateFrom)
        .dateTo(dateTo)
        .source(source)
        .processName(processName)
        .simulations(simulations)
        .viewDate(viewDate)
        .build();
  }

  public GetEntityInput toGetEntityInput(final Workflow workflow, final EntityType entityType) {
    return GetEntityInput.builder()
        .warehouseId(warehouseId)
        .workflow(workflow)
        .entityType(entityType)
        .dateFrom(dateFrom)
        .dateTo(dateTo)
        .source(source)
        .processName(processName)
        .simulations(simulations)
        .viewDate(viewDate)
        .build();
  }
}
