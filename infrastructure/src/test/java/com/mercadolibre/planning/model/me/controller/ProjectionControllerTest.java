package com.mercadolibre.planning.model.me.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.planning.model.me.entities.projection.BacklogProjection;
import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.Content;
import com.mercadolibre.planning.model.me.entities.projection.Data;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.entities.projection.chart.Chart;
import com.mercadolibre.planning.model.me.entities.projection.chart.ChartData;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTable;
import com.mercadolibre.planning.model.me.metric.DatadogMetricService;
import com.mercadolibre.planning.model.me.usecases.authorization.AuthorizeUser;
import com.mercadolibre.planning.model.me.usecases.authorization.dtos.AuthorizeUserDto;
import com.mercadolibre.planning.model.me.usecases.authorization.exceptions.UserNotAuthorizedException;
import com.mercadolibre.planning.model.me.usecases.projection.GetBacklogProjection;
import com.mercadolibre.planning.model.me.usecases.projection.GetCptProjection;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetDeferralProjection;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetProjectionInput;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.BacklogProjectionInput;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission.OUTBOUND_PROJECTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MONO_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_BATCH_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.action;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.createTabs;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.simulationMode;
import static com.mercadolibre.planning.model.me.utils.TestUtils.A_DATE;
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

@WebMvcTest(controllers = ProjectionController.class)
public class ProjectionControllerTest {

    private static final String URL = "/planning/model/middleend/workflows/%s";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthorizeUser authorizeUser;

    @MockBean
    private GetCptProjection getProjection;

    @MockBean
    private GetBacklogProjection getBacklogProjection;

    @MockBean
    private GetDeferralProjection getDeferralProjection;

    @MockBean
    private DatadogMetricService datadogMetricService;

    @Test
    void getCptProjectionOk() throws Exception {
        // GIVEN
        when(getProjection.execute(any(GetProjectionInputDto.class)))
                .thenReturn(new Projection(
                        "Test",
                        mockSuggestedWaves(),
                        mockComplexTable(),
                        mockProjectionDetailTable(),
                        mockProjectionChart(),
                        createTabs(),
                        simulationMode));

        // WHEN
        final ResultActions result = mockMvc.perform(MockMvcRequestBuilders
                .get(format(URL, FBM_WMS_OUTBOUND.getName()) + "/projections/cpt")
                .param("warehouse_id", WAREHOUSE_ID)
                .param("caller.id", String.valueOf(USER_ID))
                .contentType(APPLICATION_JSON)
        );

        // THEN
        result.andExpect(status().isOk());
        result.andExpect(content().json(getResourceAsString("get_projection_response.json")));

        verify(getProjection).execute(GetProjectionInputDto.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .build()
        );

        verify(authorizeUser).execute(new AuthorizeUserDto(USER_ID, List.of(OUTBOUND_PROJECTION)));
    }

    @Test
    void getCptProjectionUserUnauthorized() throws Exception {
        // GIVEN
        when(authorizeUser.execute(any(AuthorizeUserDto.class)))
                .thenThrow(UserNotAuthorizedException.class);

        // WHEN
        final ResultActions result = mockMvc.perform(MockMvcRequestBuilders
                .get(format(URL, FBM_WMS_OUTBOUND.getName()) + "/projections/cpt")
                .param("warehouse_id", WAREHOUSE_ID)
                .param("caller.id", String.valueOf(USER_ID))
                .contentType(APPLICATION_JSON)
        );

        // THEN
        result.andExpect(status().isForbidden());
        verifyNoInteractions(getProjection);
    }

    @Test
    void getDeferralProjection() throws Exception {
        // GIVEN
        when(getDeferralProjection.execute(new GetProjectionInput(WAREHOUSE_ID, FBM_WMS_OUTBOUND)))
                .thenReturn(new Projection(
                        "Test",
                        mockSuggestedWaves(),
                        mockComplexTable(),
                        mockProjectionDetailTable(),
                        mockProjectionChart(),
                        createTabs(),
                        simulationMode));

        // WHEN
        final ResultActions result = mockMvc.perform(MockMvcRequestBuilders
                .get(format(URL, FBM_WMS_OUTBOUND.getName()) + "/projections/deferral")
                .param("warehouse_id", WAREHOUSE_ID)
                .param("caller.id", String.valueOf(USER_ID))
                .contentType(APPLICATION_JSON)
        );

        // THEN
        result.andExpect(status().isOk());

        verify(authorizeUser).execute(new AuthorizeUserDto(USER_ID, List.of(OUTBOUND_PROJECTION)));
    }

    @Test
    void getBacklogProjectionOk() throws Exception {
        // GIVEN
        final String responseMock = getResourceAsString("get_backlog_projection_response.json");
        final ObjectMapper mapper = new ObjectMapper();

        when(getBacklogProjection.execute(any(BacklogProjectionInput.class))).thenReturn(
                mapper.readValue(responseMock, BacklogProjection.class)
        );

        // WHEN
        final ResultActions result = mockMvc.perform(MockMvcRequestBuilders
                .get(format(URL, FBM_WMS_OUTBOUND.getName()) + "/projections/backlog")
                .contentType(APPLICATION_JSON)
                .param("warehouse_id", WAREHOUSE_ID)
                .param("caller.id", String.valueOf(USER_ID))
                .param("process_name", "waving", "picking", "packing")
                .param("date_from", A_DATE.toString())
                .param("date_to", A_DATE.plusHours(25).toString())
        );

        // THEN
        result.andExpect(status().isOk());
        result.andExpect(content().json(getResourceAsString(
                "get_backlog_projection_response.json")));

        verify(authorizeUser).execute(new AuthorizeUserDto(USER_ID, List.of(OUTBOUND_PROJECTION)));
    }

    @Test
    void getBacklogProjectionUserUnauthorized() throws Exception {
        // GIVEN
        when(authorizeUser.execute(any(AuthorizeUserDto.class)))
                .thenThrow(UserNotAuthorizedException.class);

        // WHEN
        final ResultActions result = mockMvc.perform(MockMvcRequestBuilders
                .get(format(URL, FBM_WMS_OUTBOUND.getName()) + "/projections/backlog")
                .param("warehouse_id", WAREHOUSE_ID)
                .param("caller.id", String.valueOf(USER_ID))
                .param("process_name", "waving", "picking", "packing")
                .param("date_from", A_DATE.toString())
                .param("date_to", A_DATE.plusHours(25).toString())
                .contentType(APPLICATION_JSON)
        );

        // THEN
        result.andExpect(status().isForbidden());
        verifyNoInteractions(getBacklogProjection);
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
                                                        null, null, "picking"),
                                                "column_2", new Content(
                                                        "30",
                                                        ZonedDateTime.parse("2020-07-27T10:00:00Z"),
                                                        Map.of(
                                                                "title_1", "Hora de operación",
                                                                "subtitle_1", "11:00 - 12:00",
                                                                "title_2", "Cantidad de reps FCST",
                                                                "subtitle_2", "30"
                                                        ),
                                                        null
                                                )
                                        ),
                                        Map.of(
                                                "column_1", new Content("Packing",
                                                        null, null, "packing"),
                                                "column_2", new Content(
                                                        "30",
                                                        ZonedDateTime.parse("2020-07-27T10:00:00Z"),
                                                        null, null)
                                        )
                                )
                        ),
                        new Data(PRODUCTIVITY.getName(), "Productividad regular", true,
                                List.of(
                                        Map.of(
                                                "column_1", new Content("Picking",
                                                        null, null, "picking"),
                                                "column_2", new Content("30", null,
                                                        Map.of(
                                                                "title_1",
                                                                "Productividad polivalente",
                                                                "subtitle_1",
                                                                "30,4 uds/h"
                                                        ),
                                                        null
                                                )
                                        ),
                                        Map.of(
                                                "column_1", new Content("Packing",
                                                        null, null, "packing"),
                                                "column_2", new Content("30",
                                                        null, null, null)
                                        )
                                )
                        ),
                        new Data(THROUGHPUT.getName(), "Throughput", true,
                                List.of(
                                        Map.of(
                                                "column_1", new Content("Picking",
                                                        null, null, "picking"),
                                                "column_2", new Content("1600",
                                                        null, null, null)
                                        ),
                                        Map.of(
                                                "column_1", new Content("Packing",
                                                        null, null, "packing"),
                                                "column_2", new Content("1600",
                                                        null, null, null)
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
                                .projectedEndTime("2020-07-27T08:39:00Z")
                                .processingTime(new ProcessingTime(240, MINUTES.getName()))
                                .build(),
                        ChartData.builder()
                                .title("08:00")
                                .cpt("2020-07-27T08:00:00Z")
                                .projectedEndTime("2020-07-27T07:40:00Z")
                                .processingTime(new ProcessingTime(240, MINUTES.getName()))
                                .build(),
                        ChartData.builder()
                                .title("07:00")
                                .cpt("2020-07-27T07:00:00Z")
                                .projectedEndTime("2020-07-27T07:15:00Z")
                                .processingTime(new ProcessingTime(240, MINUTES.getName()))
                                .build()
                )
        );
    }

    private SimpleTable mockProjectionDetailTable() {
        return new SimpleTable(
                "Resumen de Proyección",
                List.of(
                        new ColumnHeader("column_1", "CPT's", null),
                        new ColumnHeader("column_2", "Backlog actual", null),
                        new ColumnHeader("column_3", "Desv. vs forecast", null),
                        new ColumnHeader("column_4", "Cierre proyectado", null)
                ),
                List.of(
                        Map.of(
                                "style", "none",
                                "column_1", "10:00",
                                "column_2", "57",
                                "column_3", "4%",
                                "column_4", "8:39",
                                "is_deferred", false
                        ),
                        Map.of(
                                "style", "warning",
                                "column_1", "8:00",
                                "column_2", "34",
                                "column_3", "6%",
                                "column_4", "7:40",
                                "is_deferred", false
                        ),
                        Map.of(
                                "style", "danger",
                                "column_1", "7:00",
                                "column_2", "78",
                                "column_3", "1%",
                                "column_4", "7:15",
                                "is_deferred", true
                        )
                )
        );
    }

    private SimpleTable mockSuggestedWaves() {
        final String title = "Ondas sugeridas";
        final List<ColumnHeader> columnHeaders = List.of(
                new ColumnHeader("column_1", "Sig. hora 9:00-10:00", null),
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
                ),
                Map.of("column_1",
                        Map.of("title", "Unidades por onda", "subtitle",
                                MULTI_ORDER_DISTRIBUTION.getName()),
                        "column_2", "0 uds."
                )
        );
        return new SimpleTable(title, columnHeaders, data);
    }

}
