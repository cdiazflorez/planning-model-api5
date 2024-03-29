package com.mercadolibre.planning.model.api.web.controller;

import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static java.time.ZonedDateTime.parse;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.GetSlaProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetSlaProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.activate.ActivateSimulationUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.activate.SimulationInput;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import com.mercadolibre.planning.model.api.web.controller.projection.request.QuantityByDate;
import com.mercadolibre.planning.model.api.web.controller.simulation.Simulation;
import com.mercadolibre.planning.model.api.web.controller.simulation.SimulationController;
import com.mercadolibre.planning.model.api.web.controller.simulation.SimulationEntity;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = SimulationController.class)
@SuppressWarnings("PMD.LongVariable")
public class SimulationControllerTest {

  private static final String URL = "/planning/model/workflows/{workflow}/simulations";

  @Autowired
  private MockMvc mvc;

  @MockBean
  private GetSlaProjectionUseCase getSlaProjectionUseCase;

  @MockBean
  private ActivateSimulationUseCase activateSimulationUseCase;


  @Test
  public void testSaveSimulation() throws Exception {
    // GIVEN
    final ZonedDateTime dateOut = parse("2020-01-01T10:00:00Z");
    final ZonedDateTime projectedEndDate = parse("2020-01-01T11:00:00Z");

    when(getSlaProjectionUseCase.execute(any(GetSlaProjectionInput.class)))
        .thenReturn(List.of(
            new CptProjectionOutput(dateOut, projectedEndDate, 100, null)));

    // WHEN
    final ResultActions result = mvc.perform(
        post(URL + "/save", "fbm-wms-outbound")
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("simulation_request.json"))
    );

    // THEN
    verify(activateSimulationUseCase).execute(any(SimulationInput.class));

    result.andExpect(status().isOk())
        .andExpect(jsonPath("$[0].date")
            .value(dateOut.format(ISO_OFFSET_DATE_TIME)))
        .andExpect(jsonPath("$[0].projected_end_date")
            .value(projectedEndDate.format(ISO_OFFSET_DATE_TIME)))
        .andExpect(jsonPath("$[0].remaining_quantity")
            .value(100));
  }

  @Test
  public void testRunSimulation() throws Exception {
    // GIVEN
    final ZonedDateTime dateOut = parse("2020-01-01T10:00:00Z");
    final ZonedDateTime simulatedEndDate = parse("2020-01-01T11:00:00Z");
    final ZonedDateTime projectedEndDate = parse("2020-01-01T13:00:00Z");

    when(getSlaProjectionUseCase.execute(any(GetSlaProjectionInput.class)))
        .thenReturn(List.of(
            new CptProjectionOutput(dateOut, simulatedEndDate, 100, null)))
        .thenReturn(List.of(
            new CptProjectionOutput(dateOut, projectedEndDate, 150, null)));

    // WHEN
    final ResultActions result = mvc.perform(
        post(URL + "/run", "fbm-wms-outbound")
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("simulation_request.json"))
    );

    // THEN
    verifyNoInteractions(activateSimulationUseCase);

    result.andExpect(status().isOk())
        .andExpect(jsonPath("$[0].date")
            .value(dateOut.format(ISO_OFFSET_DATE_TIME)))
        .andExpect(jsonPath("$[0].projected_end_date")
            .value(projectedEndDate.format(ISO_OFFSET_DATE_TIME)))
        .andExpect(jsonPath("$[0].simulated_end_date")
            .value(simulatedEndDate.format(ISO_OFFSET_DATE_TIME)))
        .andExpect(jsonPath("$[0].remaining_quantity")
            .value(100));
  }

  @Test
  public void testSaveSimulationDeferral() throws Exception {

    //WHEN
    final ResultActions result = mvc.perform(
        post(URL + "/deferral/save", "fbm-wms-outbound")
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("save_simulation_request.json"))
    );

    //THEN
    verify(activateSimulationUseCase).execute(SimulationInput.builder()
        .warehouseId(WAREHOUSE_ID)
        .workflow(Workflow.FBM_WMS_OUTBOUND)
        .simulations(List.of(new Simulation(
            ProcessName.GLOBAL,
            List.of(new SimulationEntity(
                EntityType.MAX_CAPACITY,
                List.of(new QuantityByDate(
                    parse("2022-07-12T08:00:00Z[UTC]"),
                    9150D,
                    null
                ))
            ))
        )))
        .userId(28251L)
        .build());

    result.andExpect(status().isOk());

  }

}
