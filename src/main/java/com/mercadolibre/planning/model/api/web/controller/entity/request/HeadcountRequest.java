package com.mercadolibre.planning.model.api.web.controller.entity.request;

import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountInput;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class HeadcountRequest extends EntityRequest {

  private Set<ProcessingType> processingType;

  public GetHeadcountInput toGetHeadcountInput(final Workflow workflow) {
    return GetHeadcountInput.builder()
        .warehouseId(warehouseId)
        .workflow(workflow)
        .dateFrom(dateFrom)
        .dateTo(dateTo)
        .source(source)
        .processName(processName)
        .processingType(processingType)
        .simulations(simulations)
        .viewDate(viewDate)
        .build();
  }
}
