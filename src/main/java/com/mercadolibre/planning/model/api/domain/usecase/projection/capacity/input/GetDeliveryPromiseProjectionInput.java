package com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.Backlog;
import com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType;
import com.mercadolibre.planning.model.api.web.controller.simulation.Simulation;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GetDeliveryPromiseProjectionInput {

  String warehouseId;

  Workflow workflow;

  ProjectionType projectionType;

  ZonedDateTime dateFrom;

  ZonedDateTime dateTo;

  List<Backlog> backlog;

  String timeZone;

  boolean applyDeviation;

  List<Simulation> simulations;
}
