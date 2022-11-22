package com.mercadolibre.planning.model.me.controller.simulation;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.action;
import static com.mercadolibre.planning.model.me.utils.TestUtils.USER_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.getResourceAsString;
import static java.lang.String.format;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.me.controller.RequestClock;
import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.Content;
import com.mercadolibre.planning.model.me.entities.projection.PlanningView;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.ResultData;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTable;
import com.mercadolibre.planning.model.me.entities.projection.complextable.Data;
import com.mercadolibre.planning.model.me.entities.projection.simulationmode.DateValidate;
import com.mercadolibre.planning.model.me.entities.projection.simulationmode.ValidatedMagnitude;
import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.QuantityByDate;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Simulation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationEntity;
import com.mercadolibre.planning.model.me.metric.DatadogMetricService;
import com.mercadolibre.planning.model.me.usecases.authorization.AuthorizeUser;
import com.mercadolibre.planning.model.me.usecases.authorization.dtos.AuthorizeUserDto;
import com.mercadolibre.planning.model.me.usecases.authorization.exceptions.UserNotAuthorizedException;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetDeferralProjection;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetProjectionInput;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.projection.simulation.RunSimulation;
import com.mercadolibre.planning.model.me.usecases.projection.simulation.SaveSimulation;
import com.mercadolibre.planning.model.me.usecases.projection.simulation.ValidateSimulationService;
import com.mercadolibre.planning.model.me.usecases.projection.simulation.WriteSimulation;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(controllers = SimulationController.class)
public class SimulationControllerTest {
  private static final String URL = "/planning/model/middleend/workflows/%s";

  final ZonedDateTime CURRENT_DATE = Instant.parse("2020-07-27T07:52:43Z").atZone(ZoneId.of("UTC"));

  final List<Map<String, Instant>> PROJECTIONS = List.of(
      Map.of(
          "CPT", Instant.parse("2020-07-27T10:00:00Z"),
          "PROJECT_END_DATE", Instant.parse("2020-07-27T09:00:00Z")
      ),
      Map.of(
          "CPT", Instant.parse("2020-07-27T11:00:00Z"),
          "PROJECT_END_DATE", Instant.parse("2020-07-27T09:45:00Z")
      ), Map.of(
          "CPT", Instant.parse("2020-07-27T12:00:00Z"),
          "PROJECT_END_DATE", Instant.parse("2020-07-27T13:10:00Z")
      )
  );

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private RunSimulation runSimulation;

  @MockBean
  private SaveSimulation saveSimulation;

  @MockBean
  private AuthorizeUser authorizeUser;

  @MockBean
  private DatadogMetricService datadogMetricService;

  @MockBean
  private RequestClock requestClock;

  @MockBean
  private ValidateSimulationService validateSimulationService;

  @MockBean
  private GetDeferralProjection getDeferralProjection;

  @MockBean
  private WriteSimulation writeSimulation;

  @Test
  void testRunSimulation() throws Exception {
    // GIVEN
    when(requestClock.now()).thenReturn(Instant.now());

    when(runSimulation.execute(any(GetProjectionInputDto.class)))
        .thenReturn(PlanningView.builder()
            .currentDate(CURRENT_DATE)
            .data(new ResultData(
                mockComplexTable(),
                mockProjectionsCpt()))
            .build()
        );

    // WHEN
    final ResultActions result = mockMvc.perform(MockMvcRequestBuilders
        .post(format(URL, FBM_WMS_OUTBOUND.getName()) + "/simulations/run")
        .param("caller.id", String.valueOf(USER_ID))
        .content(mockRunSimulationRequest())
        .contentType(APPLICATION_JSON)
    );

    // THEN
    verify(authorizeUser).execute(new AuthorizeUserDto(USER_ID,
        List.of(UserPermission.OUTBOUND_SIMULATION)));

    result.andExpect(status().isOk());
    result.andExpect(content().json(getResourceAsString(
        "get_current_projection_response2.json"
    )));
  }

  @Test
  void testRunSimulationUserUnauthorized() throws Exception {
    // GIVEN
    when(authorizeUser.execute(any(AuthorizeUserDto.class)))
        .thenThrow(UserNotAuthorizedException.class);

    // WHEN
    final ResultActions result = mockMvc.perform(MockMvcRequestBuilders
        .post(format(URL, FBM_WMS_OUTBOUND.getName()) + "/simulations/run")
        .param("warehouse_id", WAREHOUSE_ID)
        .param("caller.id", String.valueOf(USER_ID))
        .content(mockRunSimulationRequest())
        .contentType(APPLICATION_JSON)
    );

    // THEN
    result.andExpect(status().isForbidden());
    verifyNoInteractions(runSimulation);
  }

  @Test
  void testSaveSimulation() throws Exception {
    // GIVEN
    when(requestClock.now()).thenReturn(Instant.now());

    when(saveSimulation.execute(any(GetProjectionInputDto.class)))
        .thenReturn(PlanningView.builder()
            .currentDate(CURRENT_DATE)
            .data(new ResultData(
                mockComplexTable(),
                mockProjectionsCpt()))
            .build()
        );

    // WHEN
    final ResultActions result = mockMvc.perform(MockMvcRequestBuilders
        .post(format(URL, FBM_WMS_OUTBOUND.getName()) + "/simulations/save")
        .param("caller.id", String.valueOf(USER_ID))
        .content(mockRunSimulationRequest())
        .contentType(APPLICATION_JSON)
    );

    // THEN
    verify(authorizeUser).execute(new AuthorizeUserDto(USER_ID,
        List.of(UserPermission.OUTBOUND_SIMULATION)));

    result.andExpect(status().isOk());
    result.andExpect(content().json(getResourceAsString(
        "get_current_projection_response2.json"
    )));
  }

  @Test
  void testSaveSimulationUserUnauthorized() throws Exception {
    // GIVEN
    when(authorizeUser.execute(any(AuthorizeUserDto.class)))
        .thenThrow(UserNotAuthorizedException.class);

    // WHEN
    final ResultActions result = mockMvc.perform(MockMvcRequestBuilders
        .post(format(URL, FBM_WMS_OUTBOUND.getName()) + "/simulations/save")
        .param("warehouse_id", WAREHOUSE_ID)
        .param("caller.id", String.valueOf(USER_ID))
        .content(mockRunSimulationRequest())
        .contentType(APPLICATION_JSON)
    );

    // THEN
    result.andExpect(status().isForbidden());
    verifyNoInteractions(saveSimulation);
  }

  @Test
  public void testValidateSimulations() throws Exception {
    //GIVEN
    when(validateSimulationService.execute(any(GetProjectionInputDto.class)))
        .thenReturn(List.of(
            new ValidatedMagnitude("throughput",
                List.of(new DateValidate(ZonedDateTime.parse("2022-06-01T14:00:00-03:00"), true))
            )
        ));

    // WHEN
    final ResultActions result = mockMvc.perform(MockMvcRequestBuilders
        .post(format(URL, FBM_WMS_OUTBOUND.getName()) + "/simulations/deferral/validate")
        .param("caller.id", String.valueOf(USER_ID))
        .content(mockValidateSimulationRequest())
        .contentType(APPLICATION_JSON)
    );

    // THEN
    verify(authorizeUser).execute(new AuthorizeUserDto(USER_ID,
        List.of(UserPermission.OUTBOUND_SIMULATION)));

    result.andExpect(status().isOk());
    result.andExpect(content().json(
        "[{\"type\":\"throughput\",\"values\":[{\"date\":\"2022-06-01T14:00:00-03:00\",\"is_valid\":true}]}]"));

  }

  @Test
  public void testRunSimulationDeferralProjection() throws Exception {
    //GIVEN
    when(getDeferralProjection.execute(any(GetProjectionInput.class)))
        .thenReturn(PlanningView.builder()
            .currentDate(CURRENT_DATE)
            .data(new ResultData(
                null,
                null))
            .build()
        );

    // WHEN
    final ResultActions result = mockMvc.perform(MockMvcRequestBuilders
        .post(format(URL, FBM_WMS_OUTBOUND.getName()) + "/simulations/deferral/run")
        .param("caller.id", String.valueOf(USER_ID))
        .content(mockValidateSimulationRequest())
        .contentType(APPLICATION_JSON)
    );

    // THEN
    verify(authorizeUser).execute(new AuthorizeUserDto(USER_ID,
        List.of(UserPermission.OUTBOUND_SIMULATION)));

    result.andExpect(status().isOk());
    result.andExpect(status().isOk());

  }

  @Test
  public void testSaveSimulationDeferralProjection() throws Exception {
    //GIVEN
    when(getDeferralProjection.execute(any(GetProjectionInput.class)))
        .thenReturn(PlanningView.builder()
            .currentDate(CURRENT_DATE)
            .data(new ResultData(
                null,
                null))
            .build()
        );

    // WHEN
    final ResultActions result = mockMvc.perform(MockMvcRequestBuilders
        .post(format(URL, FBM_WMS_OUTBOUND.getName()) + "/simulations/deferral/save")
        .param("caller.id", String.valueOf(USER_ID))
        .content(mockValidateSimulationRequest())
        .contentType(APPLICATION_JSON)
    );

    // THEN
    verify(authorizeUser).execute(new AuthorizeUserDto(USER_ID,
        List.of(UserPermission.OUTBOUND_SIMULATION)));

    verify(writeSimulation).saveSimulations(
        FBM_WMS_OUTBOUND,
        WAREHOUSE_ID,
        List.of(new Simulation(
            ProcessName.GLOBAL,
            List.of(new SimulationEntity(
                MagnitudeType.MAX_CAPACITY,
                List.of(new QuantityByDate(
                    ZonedDateTime.parse("2020-07-27T10:00:00Z[UTC]"),
                    1
                ))
            ))
        )),
        1234L);

    result.andExpect(status().isOk());

  }

  private String mockRunSimulationRequest() throws JSONException {
    return new JSONObject()
        .put("warehouse_id", WAREHOUSE_ID)
        .put("simulations", new JSONArray().put(new JSONObject()
            .put("process_name", "picking")
            .put("entities", new JSONArray().put(new JSONObject()
                .put("type", "headcount")
                .put("values", new JSONArray().put(new JSONObject()
                    .put("date", "2020-07-27T10:00:00Z")
                    .put("quantity", 1)
                ))
            ))
        ))
        .toString();
  }

  private ComplexTable mockComplexTable() {
    return new ComplexTable(
        List.of(
            new ColumnHeader("column_1", "Horas de Operación", null)
        ),
        List.of(
            new Data(HEADCOUNT.getName(), "Headcount", true,
                List.of(
                    Map.of(
                        "column_1", new Content("Picking",
                            null, null, "picking", true),
                        "column_2", new Content(
                            "30",
                            ZonedDateTime.parse("2020-07-27T10:00:00Z"),
                            Map.of(
                                "title_1", "Hora de operación",
                                "subtitle_1", "11:00 - 12:00",
                                "title_2", "Cantidad de reps FCST",
                                "subtitle_2", "30"
                            ),
                            null, true
                        )
                    ),
                    Map.of(
                        "column_1", new Content("Packing",
                            null, null, "packing", true),
                        "column_2", new Content(
                            "30",
                            ZonedDateTime.parse("2020-07-27T10:00:00Z"),
                            null, null, true)
                    )
                )
            ),
            new Data(PRODUCTIVITY.getName(), "Productividad regular", true,
                List.of(
                    Map.of(
                        "column_1", new Content("Picking",
                            null, null, "picking", true),
                        "column_2", new Content("30", null,
                            Map.of(
                                "title_1",
                                "Productividad polivalente",
                                "subtitle_1",
                                "30,4 uds/h"
                            ),
                            null, true
                        )
                    ),
                    Map.of(
                        "column_1", new Content("Packing",
                            null, null, "packing", true),
                        "column_2", new Content("30",
                            null, null, null, true)
                    )
                )
            ),
            new Data(THROUGHPUT.getName(), "Throughput", true,
                List.of(
                    Map.of(
                        "column_1", new Content("Picking",
                            null, null, "picking", true),
                        "column_2", new Content("1600",
                            null, null, null, true)
                    ),
                    Map.of(
                        "column_1", new Content("Packing",
                            null, null, "packing", true),
                        "column_2", new Content("1600",
                            null, null, null, true)
                    )
                )
            )
        ),
        action,
        "Headcount / Throughput"
    );
  }

  private List<Projection> mockProjectionsCpt() {
    return PROJECTIONS.stream().map(elem -> new Projection(
        elem.get("CPT"),
        elem.get("PROJECT_END_DATE"),
        2,
        0L,
        0,
        60,
        0,
        false,
        false,
        0.0,
        null,
        null,
        0,
        null
    )).collect(Collectors.toList());
  }

  private String mockValidateSimulationRequest() throws JSONException {
    return new JSONObject()
        .put("warehouse_id", WAREHOUSE_ID)
        .put("simulations", new JSONArray().put(new JSONObject()
            .put("process_name", "global")
            .put("entities", new JSONArray().put(new JSONObject()
                .put("type", "max_capacity")
                .put("values", new JSONArray().put(new JSONObject()
                    .put("date", "2020-07-27T10:00:00Z")
                    .put("quantity", 1)
                ))
            ))
        ))
        .toString();
  }
}
