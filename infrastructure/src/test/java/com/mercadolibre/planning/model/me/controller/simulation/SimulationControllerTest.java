package com.mercadolibre.planning.model.me.controller.simulation;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MONO_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_BATCH_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.action;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.createOutboundTabs;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.simulationMode;
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
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.entities.projection.Tab;
import com.mercadolibre.planning.model.me.entities.projection.chart.Chart;
import com.mercadolibre.planning.model.me.entities.projection.chart.ChartData;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTable;
import com.mercadolibre.planning.model.me.entities.projection.complextable.Data;
import com.mercadolibre.planning.model.me.entities.projection.simulationmode.DateValidate;
import com.mercadolibre.planning.model.me.entities.projection.simulationmode.ValidatedMagnitude;
import com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission;
import com.mercadolibre.planning.model.me.metric.DatadogMetricService;
import com.mercadolibre.planning.model.me.usecases.authorization.AuthorizeUser;
import com.mercadolibre.planning.model.me.usecases.authorization.dtos.AuthorizeUserDto;
import com.mercadolibre.planning.model.me.usecases.authorization.exceptions.UserNotAuthorizedException;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetDeferralProjection;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetProjectionInput;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.projection.simulation.RunSimulation;
import com.mercadolibre.planning.model.me.usecases.projection.simulation.SaveSimulation;
import com.mercadolibre.planning.model.me.usecases.projection.simulation.ValidateSimulation;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
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
    private ValidateSimulation validateSimulation;

    @MockBean
    private GetDeferralProjection getDeferralProjection;

    @Test
    void testRunSimulation() throws Exception {
        // GIVEN

        when(requestClock.now()).thenReturn(Instant.now());

        when(runSimulation.execute(any(GetProjectionInputDto.class)))
                .thenReturn(new Projection("Test",
                        null,
                        new com.mercadolibre.planning.model.me.entities.projection.Data(
                                mockSuggestedWaves(),
                                mockComplexTable(),
                                null,
                                mockProjectionChart()),
                        createOutboundTabs(),
                        simulationMode));

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
                "get_current_projection_response.json"
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
                .thenReturn(new Projection("Test",
                        null,
                        new com.mercadolibre.planning.model.me.entities.projection.Data(
                                mockSuggestedWaves(),
                                mockComplexTable(),
                                null,
                                mockProjectionChart()),
                        createOutboundTabs(),
                        simulationMode));

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
                "get_current_projection_response.json"
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
        when(validateSimulation.execute(any(GetProjectionInputDto.class)))
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
                .thenReturn(new Projection("Projection",
                        null,
                        new com.mercadolibre.planning.model.me.entities.projection.Data(null,
                                null,
                                null,
                                null),
                        List.of(
                                new Tab("cpt", "Cumplimiento de CPTs")),
                        null

                ));

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

    private Chart mockProjectionChart() {
        return new Chart(
                List.of(
                        ChartData.builder()
                                .title("10:00")
                                .cpt("2020-07-27T10:00:00Z")
                                .projectedEndTime("2020-07-27T09:00:00Z")
                                .processingTime(new ProcessingTime(240, MINUTES.getName()))
                                .build(),
                        ChartData.builder()
                                .title("11:00")
                                .cpt("2020-07-27T11:00:00Z")
                                .projectedEndTime("2020-07-27T09:45:00Z")
                                .processingTime(new ProcessingTime(240, MINUTES.getName()))
                                .build(),
                        ChartData.builder()
                                .title("12:00")
                                .cpt("2020-07-27T12:00:00Z")
                                .projectedEndTime("2020-07-27T13:10:00Z")
                                .processingTime(new ProcessingTime(240, MINUTES.getName()))
                                .build()
                )
        );
    }

    private SimpleTable mockSuggestedWaves() {
        final String title = "Ondas sugeridas";
        final List<ColumnHeader> columnHeaders = List.of(
                new ColumnHeader("column_1", "Sig. hora 23:00-1:00", null),
                new ColumnHeader("column_2", "Tamaño de onda", null)
        );
        final List<Map<String, Object>> data = List.of(
                Map.of("column_1",
                        Map.of("title", "Unidades por onda", "subtitle",
                                MONO_ORDER_DISTRIBUTION.getName()),
                        "column_2", "130 uds."
                ),
                Map.of("column_1",
                        Map.of("title", "Unidades por onda", "subtitle",
                                MULTI_BATCH_DISTRIBUTION.getName()),
                        "column_2", "0 uds."
                )
        );
        return new SimpleTable(title, columnHeaders, data);
    }

    private String mockValidateSimulationRequest() throws JSONException {
        return new JSONObject()
                .put("warehouse_id", WAREHOUSE_ID)
                .put("simulations", new JSONArray().put(new JSONObject()
                        .put("process_name", "global")
                        .put("entities", new JSONArray().put(new JSONObject()
                                .put("type", "throughput")
                                .put("values", new JSONArray().put(new JSONObject()
                                        .put("date", "2020-07-27T10:00:00Z")
                                        .put("quantity", 1)
                                ))
                        ))
                ))
                .toString();
    }
}
