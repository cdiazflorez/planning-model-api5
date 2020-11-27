package com.mercadolibre.planning.model.api.web.controller;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ForecastMetadataView;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetForecastMetadataInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.GetForecastMetadataUseCase;
import com.mercadolibre.planning.model.api.web.controller.editor.MetricUnitEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.api.web.controller.request.GetForecastMetadataRequest;
import lombok.AllArgsConstructor;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/workflows/{workflow}/metadata")
public class MetadataController {

    private final GetForecastMetadataUseCase getForecastMetadataUseCase;

    @GetMapping
    public ResponseEntity<List<ForecastMetadataView>> getLastHistoricForecast(
            @PathVariable final Workflow workflow,
            @Valid final GetForecastMetadataRequest request) {
        final GetForecastMetadataInput input = request.getForecastMetadataInput(workflow);
        return ResponseEntity.status(HttpStatus.OK)
                .body(getForecastMetadataUseCase.execute(input));
    }

    @InitBinder
    public void initBinder(final PropertyEditorRegistry dataBinder) {
        dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
        dataBinder.registerCustomEditor(MetricUnit.class, new MetricUnitEditor());
    }
}
