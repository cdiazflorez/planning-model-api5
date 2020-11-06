package com.mercadolibre.planning.model.api.web.controller.request;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetPlanningDistributionInput;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import java.time.ZonedDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class GetPlanningDistributionRequest {

    @NotBlank
    private String warehouseId;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private ZonedDateTime dateOutFrom;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private ZonedDateTime dateOutTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private ZonedDateTime dateInTo;

    public GetPlanningDistributionInput toGetPlanningDistInput(final Workflow workflow) {
        return GetPlanningDistributionInput.builder()
                .warehouseId(warehouseId)
                .workflow(workflow)
                .dateOutFrom(dateOutFrom)
                .dateOutTo(dateOutTo)
                .dateInTo(dateInTo)
                .build();
    }
}