package com.mercadolibre.planning.model.me.controller;

import static com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission.OUTBOUND_PROJECTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MONO_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_BATCH_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_ORDER_DISTRIBUTION;
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

import com.google.common.collect.Lists;
import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.Content;
import com.mercadolibre.planning.model.me.entities.projection.PlanningView;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.ResultData;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTable;
import com.mercadolibre.planning.model.me.entities.projection.complextable.Data;
import com.mercadolibre.planning.model.me.entities.projection.dateselector.Date;
import com.mercadolibre.planning.model.me.entities.projection.dateselector.DateSelector;
import com.mercadolibre.planning.model.me.metric.DatadogMetricService;
import com.mercadolibre.planning.model.me.usecases.authorization.AuthorizeUser;
import com.mercadolibre.planning.model.me.usecases.authorization.dtos.AuthorizeUserDto;
import com.mercadolibre.planning.model.me.usecases.authorization.exceptions.UserNotAuthorizedException;
import com.mercadolibre.planning.model.me.usecases.projection.GetSlaProjection;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetDeferralProjection;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetProjectionInput;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(controllers = ProjectionController.class)
public class ProjectionControllerTest {
  private static final String URL = "/planning/model/middleend/workflows/%s";
  final ZonedDateTime CURRENT_DATE = Instant.parse("2022-07-12T01:52:43Z").atZone(ZoneId.of("UTC"));
  final List<Map<String, Instant>> PROJECTIONS = List.of(
      Map.of(
          "CPT", Instant.parse("2022-07-12T15:00:00Z"),
          "PROJECT_END_DATE", Instant.parse("2022-07-12T16:00:00Z")
      ),
      Map.of(
          "CPT", Instant.parse("2022-07-12T21:00:00Z"),
          "PROJECT_END_DATE", Instant.parse("2022-07-12T22:00:00Z")
      ), Map.of(
          "CPT", Instant.parse("2022-07-12T23:00:00Z"),
          "PROJECT_END_DATE", Instant.parse("2022-07-12T23:57:00Z")
      )
  );

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private AuthorizeUser authorizeUser;

  @MockBean
  private GetSlaProjection getSlaProjection;

  @MockBean
  private GetDeferralProjection getDeferralProjection;

  @MockBean
  private DatadogMetricService datadogMetricService;

  @MockBean
  private RequestClock requestClock;

  @Test
  void getCptProjectionOk() throws Exception {
    // GIVEN
    final Instant now = Instant.now();

    when(requestClock.now()).thenReturn(now);
    when(getSlaProjection.execute(any(GetProjectionInputDto.class)))
        .thenReturn(PlanningView.builder()
                        .isNewVersion(true)
                        .currentDate(CURRENT_DATE)
                        .dateSelector(mockDateSelector())
                        .data(new ResultData(
                            mockSuggestedWaves(),
                            mockComplexTable(),
                            mockProjectionsCpt()))
                        .build()
        );

    // WHEN
    final ResultActions result = mockMvc.perform(MockMvcRequestBuilders
                                                     .get(format(URL, FBM_WMS_OUTBOUND.getName()) + "/projections/cpt")
                                                     .param("warehouse_id", WAREHOUSE_ID)
                                                     .param("caller.id", String.valueOf(USER_ID))
                                                     .contentType(APPLICATION_JSON)
    );

    // THEN
    result.andExpect(status().isOk());
    result.andExpect(content().json(getResourceAsString("get_projection_cpt_response.json")));

    verify(getSlaProjection).execute(GetProjectionInputDto.builder()
                                         .workflow(FBM_WMS_OUTBOUND)
                                         .warehouseId(WAREHOUSE_ID)
                                         .requestDate(now)
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
    verifyNoInteractions(getSlaProjection);
  }

  @Test
  void getDeferralProjection() throws Exception {
    // GIVEN
    when(getDeferralProjection.execute(
        new GetProjectionInput(WAREHOUSE_ID, FBM_WMS_OUTBOUND, null, any(), false, Collections.emptyList())))
        .thenReturn(PlanningView.builder()
                        .isNewVersion(true)
                        .currentDate(CURRENT_DATE)
                        .dateSelector(mockDateSelector())
                        .data(new ResultData(
                            null,
                            mockComplexTable(),
                            mockProjectionsDeferral()))
                        .build()
        );

    // WHEN
    final ResultActions result = mockMvc.perform(MockMvcRequestBuilders
                                                     .get(format(URL, FBM_WMS_OUTBOUND.getName()) + "/projections/deferral")
                                                     .param("warehouse_id", WAREHOUSE_ID)
                                                     .param("caller.id", String.valueOf(USER_ID))
                                                     .contentType(APPLICATION_JSON)
    );

    // THEN
    result.andExpect(status().isOk());
    result.andExpect(content().json(getResourceAsString("get_projection_response2.json")));

    verify(authorizeUser).execute(new AuthorizeUserDto(USER_ID, List.of(OUTBOUND_PROJECTION)));
  }

  @Test
  void getDeferralProjection21Cap5Logic() throws Exception {
    // GIVEN
    when(getDeferralProjection.execute(
        new GetProjectionInput(WAREHOUSE_ID, FBM_WMS_OUTBOUND, null, null, true, Collections.emptyList())))
        .thenReturn(PlanningView.builder()
                        .isNewVersion(true)
                        .currentDate(CURRENT_DATE)
                        .dateSelector(mockDateSelector())
                        .data(new ResultData(
                            null,
                            mockComplexTable(),
                            mockProjectionsDeferral()))
                        .build()
        );

    // WHEN
    final ResultActions result = mockMvc.perform(MockMvcRequestBuilders
                                                     .get(format(URL, FBM_WMS_OUTBOUND.getName()) + "/projections/deferral")
                                                     .param("warehouse_id", WAREHOUSE_ID)
                                                     .param("cap_5_to_pack", "true")
                                                     .param("caller.id", String.valueOf(USER_ID))
                                                     .contentType(APPLICATION_JSON)
    );

    // THEN
    result.andExpect(status().isOk());

    verify(authorizeUser).execute(new AuthorizeUserDto(USER_ID, List.of(OUTBOUND_PROJECTION)));
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
        2L,
        0L,
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

  private List<Projection> mockProjectionsDeferral() {

    return Lists.reverse(PROJECTIONS).stream().map(elem -> new Projection(
        elem.get("CPT"),
        elem.get("PROJECT_END_DATE"),
        2L,
        null,
        60,
        0,
        false,
        false,
        null,
        null,
        null,
        0,
        null
    )).collect(Collectors.toList());
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

  private DateSelector mockDateSelector() {
    final Date[] dates = {
        new Date("2021-09-06T02:00:00Z", "Lunes 06/09/2021", true),
        new Date("2021-09-07T02:00:00Z", "Martes 07/09/2021", false),
        new Date("2021-09-08T02:00:00Z", "Miercoles 08/09/2021", false),
        new Date("2021-09-09T02:00:00Z", "Jueves 09/09/2021", false)
    };

    return new DateSelector("Fecha:", dates);
  }
}