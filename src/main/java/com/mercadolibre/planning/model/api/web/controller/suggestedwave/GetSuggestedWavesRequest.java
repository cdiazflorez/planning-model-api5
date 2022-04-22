package com.mercadolibre.planning.model.api.web.controller.suggestedwave;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.suggestedwave.get.GetSuggestedWavesInput;
import java.time.ZonedDateTime;
import javax.validation.constraints.NotBlank;
import lombok.Value;
import org.springframework.format.annotation.DateTimeFormat;

@Value
public class GetSuggestedWavesRequest {

  @NotBlank
  private String warehouseId;

  @NotBlank
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private String dateFrom;

  @NotBlank
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private String dateTo;

  @NotBlank
  private String backlog;

  private boolean applyDeviation;

  public GetSuggestedWavesInput getSuggestedWavesInput(final Workflow workflow) {
    return GetSuggestedWavesInput.builder()
        .warehouseId(warehouseId)
        .workflow(workflow)
        .dateFrom(ZonedDateTime.parse(dateFrom))
        .dateTo(ZonedDateTime.parse(dateTo))
        .backlog(Long.parseLong(backlog))
        .applyDeviation(applyDeviation)
        .build();
  }
}
