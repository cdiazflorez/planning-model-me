package com.mercadolibre.planning.model.me.controller;

import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.ComplexTable;
import com.mercadolibre.planning.model.me.entities.projection.Content;
import com.mercadolibre.planning.model.me.entities.projection.Data;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.entities.projection.chart.Chart;
import com.mercadolibre.planning.model.me.entities.projection.chart.ChartData;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
import com.mercadolibre.planning.model.me.usecases.authorization.AuthorizeUser;
import com.mercadolibre.planning.model.me.usecases.authorization.dtos.AuthorizeUserDto;
import com.mercadolibre.planning.model.me.usecases.authorization.exceptions.UserNotAuthorizedException;
import com.mercadolibre.planning.model.me.usecases.projection.GetProjection;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission.WAVE_WRITE;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.TestUtils.USER_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.getResourceAsString;
import static java.lang.String.format;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
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
    private GetProjection getProjection;

    @Test
    void getProjectionOk() throws Exception {
        // GIVEN
        doNothing().when(authorizeUser).execute(AuthorizeUserDto.builder()
                .userId(USER_ID)
                .requiredPermissions(List.of(WAVE_WRITE))
                .build()
        );

        when(getProjection.execute(any(GetProjectionInputDto.class)))
                .thenReturn(new Projection(
                        "Test",
                        mockComplexTable(),
                        mockProjectionDetailTable(),
                        mockProjectionChart()));

        // WHEN
        final ResultActions result = mockMvc.perform(MockMvcRequestBuilders
                .get(format(URL, FBM_WMS_OUTBOUND.getName()) + "/projections")
                .param("warehouse_id", WAREHOUSE_ID)
                .param("caller.id", String.valueOf(USER_ID))
                .contentType(APPLICATION_JSON)
        );

        // THEN
        result.andExpect(status().isOk());
        result.andExpect(content().json(getResourceAsString("get_projection_response.json")));

        Mockito.verify(getProjection).execute(GetProjectionInputDto.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .build()
        );
    }

    @Test
    void getProjectionUserUnauthorized() throws Exception {
        // GIVEN
        when(authorizeUser.execute(any(AuthorizeUserDto.class)))
                .thenThrow(UserNotAuthorizedException.class);

        // WHEN
        final ResultActions result = mockMvc.perform(MockMvcRequestBuilders
                .get(format(URL, FBM_WMS_OUTBOUND.getName()) + "/projections")
                .param("warehouse_id", WAREHOUSE_ID)
                .param("caller.id", String.valueOf(USER_ID))
                .contentType(APPLICATION_JSON)
        );

        // THEN
        result.andExpect(status().isForbidden());
        verifyNoInteractions(getProjection);
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
                        new ChartData(
                                "10:00",
                                "2020-07-27T10:00:00Z",
                                "2020-07-27T08:39:00Z"
                        ),
                        new ChartData(
                                "08:00",
                                "2020-07-27T08:00:00Z",
                                "2020-07-27T07:40:00Z"
                        ),
                        new ChartData(
                                "07:00",
                                "2020-07-27T07:00:00Z",
                                "2020-07-27T07:15:00Z"
                        )
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
                                "column_3", "4",
                                "column_4", "8:39"
                        ),
                        Map.of(
                                "style", "warning",
                                "column_1", "8:00",
                                "column_2", "34",
                                "column_3", "6",
                                "column_4", "7:40"
                        ),
                        Map.of(
                                "style", "danger",
                                "column_1", "7:00",
                                "column_2", "78",
                                "column_3", "1",
                                "column_4", "7:15"
                        )
                )
        );
    }

}
