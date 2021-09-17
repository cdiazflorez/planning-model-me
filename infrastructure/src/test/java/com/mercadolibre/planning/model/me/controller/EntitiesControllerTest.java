package com.mercadolibre.planning.model.me.controller;

import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.Content;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTable;
import com.mercadolibre.planning.model.me.entities.projection.complextable.Data;
import com.mercadolibre.planning.model.me.usecases.authorization.AuthorizeUser;
import com.mercadolibre.planning.model.me.usecases.authorization.dtos.AuthorizeUserDto;
import com.mercadolibre.planning.model.me.usecases.authorization.exceptions.UserNotAuthorizedException;
import com.mercadolibre.planning.model.me.usecases.projection.GetEntities;
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
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.action;
import static com.mercadolibre.planning.model.me.utils.TestUtils.USER_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.getResourceAsString;
import static java.lang.String.format;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EntitiesController.class)
public class EntitiesControllerTest {

    private static final String URL = "/planning/model/middleend/warehouses/%s/workflows/%s";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthorizeUser authorizeUser;

    @MockBean
    private GetEntities getEntities;

    @Test
    void getEntitiesOk() throws Exception {
        // GIVEN
        when(getEntities.execute(GetProjectionInputDto.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .build()))
                .thenReturn(mockComplexTable());

        // WHEN
        final ResultActions result = mockMvc.perform(MockMvcRequestBuilders
                .get(format(URL, WAREHOUSE_ID, FBM_WMS_OUTBOUND.getName()) + "/entities")
                .param("caller.id", String.valueOf(USER_ID))
                .contentType(APPLICATION_JSON)
        );

        // THEN
        result.andExpect(status().isOk());
        result.andExpect(content().json(getResourceAsString("get_entities_response.json")));

        verify(authorizeUser).execute(new AuthorizeUserDto(USER_ID, List.of(OUTBOUND_PROJECTION)));
    }

    @Test
    void getEntitiesUnauthorized() throws Exception {
        // GIVEN
        when(authorizeUser.execute(new AuthorizeUserDto(USER_ID, List.of(OUTBOUND_PROJECTION))))
                .thenThrow(new UserNotAuthorizedException(
                        format("User %s has no permissions.", USER_ID)));

        // WHEN
        final ResultActions result = mockMvc.perform(MockMvcRequestBuilders
                .get(format(URL, WAREHOUSE_ID, FBM_WMS_OUTBOUND.getName()) + "/entities")
                .param("caller.id", String.valueOf(USER_ID))
                .contentType(APPLICATION_JSON)
        );

        // THEN
        result.andExpect(status().isForbidden());
        verifyNoInteractions(getEntities);
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
}
