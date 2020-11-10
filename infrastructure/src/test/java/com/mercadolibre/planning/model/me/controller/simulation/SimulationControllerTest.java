package com.mercadolibre.planning.model.me.controller.simulation;

import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.ComplexTable;
import com.mercadolibre.planning.model.me.entities.projection.Content;
import com.mercadolibre.planning.model.me.entities.projection.Data;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.chart.Chart;
import com.mercadolibre.planning.model.me.entities.projection.chart.ChartData;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.projection.simulation.RunSimulation;
import com.mercadolibre.planning.model.me.usecases.projection.simulation.SaveSimulation;
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

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.TestUtils.USER_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.lang.String.format;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SimulationController.class)
public class SimulationControllerTest {

    private static final String URL = "/planning/model/middleend/workflows/%s";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RunSimulation runSimulation;

    @MockBean
    private SaveSimulation saveSimulation;

    @Test
    void testRunSimulation() throws Exception {
        // GIVEN
        when(runSimulation.execute(any(GetProjectionInputDto.class)))
                .thenReturn(new Projection("Test", mockComplexTable(), null,
                        mockProjectionChart()));

        // WHEN
        final ResultActions result = mockMvc.perform(MockMvcRequestBuilders
                .post(format(URL, FBM_WMS_OUTBOUND.getName()) + "/simulations/run")
                .param("caller.id", String.valueOf(USER_ID))
                .content(mockRunSimulationRequest())
                .contentType(APPLICATION_JSON)
        );

        // THEN
        result.andExpect(status().isOk());
        result.andExpect(content().json(complexTableJsonResponse()));
    }

    @Test
    void testSaveSimulation() throws Exception {
        // GIVEN
        when(saveSimulation.execute(any(GetProjectionInputDto.class)))
                .thenReturn(new Projection("Test", mockComplexTable(), null,
                        mockProjectionChart()));

        // WHEN
        final ResultActions result = mockMvc.perform(MockMvcRequestBuilders
                .post(format(URL, FBM_WMS_OUTBOUND.getName()) + "/simulations/save")
                .param("caller.id", String.valueOf(USER_ID))
                .content(mockRunSimulationRequest())
                .contentType(APPLICATION_JSON)
        );

        // THEN
        result.andExpect(status().isOk());
        result.andExpect(content().json(complexTableJsonResponse()));
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
                        new ColumnHeader("column_1", "Hora de Operacion", null)
                ),
                List.of(
                        new Data(HEADCOUNT.getName(), "Headcount", true,
                                List.of(
                                        Map.of(
                                                "column_1", new Content("Picking", null, null),
                                                "column_2", new Content(
                                                        "30",
                                                        ZonedDateTime.parse("2020-07-27T10:00:00Z"),
                                                        Map.of(
                                                                "title_1", "Hora de operacion",
                                                                "subtitle_1", "11:00 - 12:00",
                                                                "title_2", "Cantidad de reps FCST",
                                                                "subtitle_2", "30"
                                                        )
                                                )
                                        ),
                                        Map.of(
                                                "column_1", new Content("Packing", null, null),
                                                "column_2", new Content(
                                                        "30",
                                                        ZonedDateTime.parse("2020-07-27T10:00:00Z"),
                                                        null)
                                        )
                                )
                        ),
                        new Data(PRODUCTIVITY.getName(), "Productivity", true,
                                List.of(
                                        Map.of(
                                                "column_1", new Content("Picking", null, null),
                                                "column_2", new Content("30", null,
                                                        Map.of(
                                                                "title_1",
                                                                "Productividad polivalente",
                                                                "subtitle_1",
                                                                "30,4 uds/h"
                                                        )
                                                )
                                        ),
                                        Map.of(
                                                "column_1", new Content("Packing", null, null),
                                                "column_2", new Content("30", null, null)
                                        )
                                )
                        ),
                        new Data(THROUGHPUT.getName(), "Throughput", true,
                                List.of(
                                        Map.of(
                                                "column_1", new Content("Picking", null, null),
                                                "column_2", new Content("1600", null, null)
                                        ),
                                        Map.of(
                                                "column_1", new Content("Packing", null, null),
                                                "column_2", new Content("1600", null, null)
                                        )
                                )
                        )
                )
        );
    }

    private Chart mockProjectionChart() {
        return new Chart(
                new ProcessingTime(60, MINUTES.getName()),
                List.of(
                        ChartData.builder()
                                .title("10:00")
                                .cpt("2020-07-27T10:00:00Z")
                                .projectedEndTime("2020-07-27T09:00:00Z")
                                .build(),
                        ChartData.builder()
                                .title("11:00")
                                .cpt("2020-07-27T11:00:00Z")
                                .projectedEndTime("2020-07-27T09:45:00Z")
                                .build(),
                        ChartData.builder()
                                .title("12:00")
                                .cpt("2020-07-27T12:00:00Z")
                                .projectedEndTime("2020-07-27T13:10:00Z")
                                .build()
                )
        );
    }

    private String complexTableJsonResponse() {
        return "{\n"
                + "   \"title\":\"Test\",\n"
                + "   \"complex_table_1\":{\n"
                + "      \"columns\":[\n"
                + "         {\n"
                + "            \"id\":\"column_1\",\n"
                + "            \"title\":\"Hora de Operacion\"\n"
                + "         }\n"
                + "      ],\n"
                + "      \"data\":[\n"
                + "         {\n"
                + "            \"id\":\"headcount\",\n"
                + "            \"title\":\"Headcount\",\n"
                + "            \"open\":true,\n"
                + "            \"content\":[\n"
                + "               {\n"
                + "                  \"column_1\":{\n"
                + "                     \"title\":\"Picking\"\n"
                + "                  },\n"
                + "                  \"column_2\":{\n"
                + "                     \"title\":\"30\",\n"
                + "                     \"date\":\"2020-07-27T10:00:00Z\",\n"
                + "                     \"tooltip\":{\n"
                + "                        \"title_1\":\"Hora de operacion\",\n"
                + "                        \"subtitle_1\":\"11:00 - 12:00\",\n"
                + "                        \"title_2\":\"Cantidad de reps FCST\",\n"
                + "                        \"subtitle_2\":\"30\"\n"
                + "                     }\n"
                + "                  }\n"
                + "               },\n"
                + "               {\n"
                + "                  \"column_1\":{\n"
                + "                     \"title\":\"Packing\"\n"
                + "                  },\n"
                + "                  \"column_2\":{\n"
                + "                     \"title\":\"30\",\n"
                + "                     \"date\":\"2020-07-27T10:00:00Z\"\n"
                + "                  }\n"
                + "               }\n"
                + "            ]\n"
                + "         },\n"
                + "         {\n"
                + "            \"id\":\"productivity\",\n"
                + "            \"title\":\"Productivity\",\n"
                + "            \"open\":true,\n"
                + "            \"content\":[\n"
                + "               {\n"
                + "                  \"column_1\":{\n"
                + "                     \"title\":\"Picking\"\n"
                + "                  },\n"
                + "                  \"column_2\":{\n"
                + "                     \"title\":\"30\",\n"
                + "                     \"tooltip\":{\n"
                + "                        \"title_1\":\"Productividad polivalente\",\n"
                + "                        \"subtitle_1\":\"30,4 uds/h\"\n"
                + "                     }\n"
                + "                  }\n"
                + "               },\n"
                + "               {\n"
                + "                  \"column_1\":{\n"
                + "                     \"title\":\"Packing\"\n"
                + "                  },\n"
                + "                  \"column_2\":{\n"
                + "                     \"title\":\"30\"\n"
                + "                  }\n"
                + "               }\n"
                + "            ]\n"
                + "         },\n"
                + "         {\n"
                + "            \"id\":\"throughput\",\n"
                + "            \"title\":\"Throughput\",\n"
                + "            \"open\":true,\n"
                + "            \"content\":[\n"
                + "               {\n"
                + "                  \"column_1\":{\n"
                + "                     \"title\":\"Picking\"\n"
                + "                  },\n"
                + "                  \"column_2\":{\n"
                + "                     \"title\":\"1600\"\n"
                + "                  }\n"
                + "               },\n"
                + "               {\n"
                + "                  \"column_1\":{\n"
                + "                     \"title\":\"Packing\"\n"
                + "                  },\n"
                + "                  \"column_2\":{\n"
                + "                     \"title\":\"1600\"\n"
                + "                  }\n"
                + "               }\n"
                + "            ]\n"
                + "         }\n"
                + "      ]\n"
                + "   }\n"
                + "}";
    }
}
