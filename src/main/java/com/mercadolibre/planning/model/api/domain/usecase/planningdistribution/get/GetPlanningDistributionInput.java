package com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@Builder
public class GetPlanningDistributionInput {

    private String warehouseId;
    private Workflow workflow;
    private ZonedDateTime dateOutFrom;
    private ZonedDateTime dateOutTo;
    private ZonedDateTime dateInFrom;
    private ZonedDateTime dateInTo;
    private boolean applyDeviation;

}
