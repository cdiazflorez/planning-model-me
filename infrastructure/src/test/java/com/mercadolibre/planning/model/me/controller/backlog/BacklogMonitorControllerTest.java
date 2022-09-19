package com.mercadolibre.planning.model.me.controller.backlog;

import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getDateSelector;
import static com.mercadolibre.planning.model.me.utils.TestUtils.getResourceAsString;
import static java.lang.String.format;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.me.controller.RequestClock;
import com.mercadolibre.planning.model.me.entities.monitor.AreaBacklogDetail;
import com.mercadolibre.planning.model.me.entities.monitor.DetailedBacklogPhoto;
import com.mercadolibre.planning.model.me.entities.monitor.ProcessDetail;
import com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure;
import com.mercadolibre.planning.model.me.entities.monitor.VariablesPhoto;
import com.mercadolibre.planning.model.me.entities.monitor.WorkflowBacklogDetail;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogMonitor;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogMonitorDetails;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorDetailsInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorDetailsResponse;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorInputDto;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(controllers = BacklogMonitorController.class)
class BacklogMonitorControllerTest {
  private static final String BASE_URL = "/wms/flow/middleend/logistic_centers/%s/backlog";

  private static final String CURRENT_DATE = "2021-08-12T01:00:00Z";

  private static final String DATE_FROM = "2021-08-13T01:00:00Z";

  private static final String DATE_FROM_MINUS_TWO_HOURS = "2021-08-12T23:00:00Z";

  private static final String A_DATE_MINUS_TWO_HOURS = "2021-08-11T23:00:00Z";

  private static final String ANOTHER_DATE = "2021-08-12T04:00:00Z";

  private static final String WAREHOUSE_ID = "COCU01";

  private static final String PROCESS = "picking";

  private static final String OUTBOUND = "fbm-wms-outbound";

  private static final TimeZone TIME_ZONE = TimeZone.getTimeZone("America/Argentina/Buenos_Aires");

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private GetBacklogMonitor getBacklogMonitor;

  @MockBean
  private GetBacklogMonitorDetails getBacklogMonitorDetails;

  @MockBean
  private RequestClock requestClock;

  @Test
  void testGetMonitorWithProcess() throws Exception {
    // GIVEN

    var firstDate = OffsetDateTime.parse(CURRENT_DATE, ISO_DATE_TIME).toInstant();
    GetBacklogMonitorInputDto input = new GetBacklogMonitorInputDto(
        firstDate,
        WAREHOUSE_ID,
        FBM_WMS_OUTBOUND,
        List.of(WAVING, PICKING, PACKING),
        OffsetDateTime.parse(DATE_FROM_MINUS_TWO_HOURS, ISO_DATE_TIME).toInstant(),
        OffsetDateTime.parse(ANOTHER_DATE, ISO_DATE_TIME).toInstant(),
        999L,
        false);

    when(requestClock.now()).thenReturn(firstDate);
    when(getBacklogMonitor.execute(input)).thenReturn(getMockedResponse());

    // WHEN
    final ResultActions result = mockMvc.perform(
        MockMvcRequestBuilders.get(format(BASE_URL, WAREHOUSE_ID) + "/monitor")
            .param("workflow", OUTBOUND)
            .param("date_from", DATE_FROM)
            .param("date_to", ANOTHER_DATE)
            .param("caller.id", "999")
            .param("processes", "WAVING, PICKING, PACKING")
    );

    // THEN
    result.andExpect(status().isOk());
    result.andExpect(content().json(
        getResourceAsString("get_backlog_monitor_response.json")));
  }

  @Test
  void testGetMonitorWithoutFromDate() throws Exception {
    var firstDate = OffsetDateTime.parse(CURRENT_DATE, ISO_DATE_TIME).toInstant();
    var dateTo = firstDate.plus(Duration.ofHours(21));
    GetBacklogMonitorInputDto input = new GetBacklogMonitorInputDto(
        firstDate,
        WAREHOUSE_ID,
        FBM_WMS_OUTBOUND,
        List.of(WAVING, PICKING, PACKING),
        OffsetDateTime.parse(A_DATE_MINUS_TWO_HOURS, ISO_DATE_TIME).toInstant(),
        dateTo,
        999L,
        false);

    when(requestClock.now()).thenReturn(firstDate);
    when(getBacklogMonitor.execute(input)).thenReturn(getMockedResponse());

    final ResultActions result = mockMvc.perform(
        MockMvcRequestBuilders.get(format(BASE_URL, WAREHOUSE_ID) + "/monitor")
            .param("workflow", OUTBOUND)
            .param("caller.id", "999")
            .param("processes", "WAVING, PICKING, PACKING")
    );

    // THEN
    result.andExpect(status().isOk());
    result.andExpect(content().json(
        getResourceAsString("get_backlog_monitor_response.json")));
  }

  @Test
  void testGetMonitorWithoutProcesses() throws Exception {
    // GIVEN

    var firstDate = OffsetDateTime.parse(CURRENT_DATE, ISO_DATE_TIME).toInstant();
    GetBacklogMonitorInputDto input = new GetBacklogMonitorInputDto(
        firstDate,
        WAREHOUSE_ID,
        FBM_WMS_OUTBOUND,
        List.of(WAVING, PICKING, PACKING),
        OffsetDateTime.parse(A_DATE_MINUS_TWO_HOURS, ISO_DATE_TIME).toInstant(),
        OffsetDateTime.parse(ANOTHER_DATE, ISO_DATE_TIME).toInstant(),
        999L,
        false);

    when(requestClock.now()).thenReturn(firstDate);
    when(getBacklogMonitor.execute(input)).thenReturn(getMockedResponse());

    // WHEN
    final ResultActions result = mockMvc.perform(
        MockMvcRequestBuilders.get(format(BASE_URL, WAREHOUSE_ID) + "/monitor")
            .param("workflow", OUTBOUND)
            .param("date_from", CURRENT_DATE)
            .param("date_to", ANOTHER_DATE)
            .param("caller.id", "999")
            .param("processes", "")
    );

    // THEN
    result.andExpect(status().isOk());
    result.andExpect(content().json(
        getResourceAsString("get_backlog_monitor_response.json")));
  }

  @Test
  void testGetMonitorEmptyProcesses() throws Exception {
    // GIVEN

    var firstDate = OffsetDateTime.parse(CURRENT_DATE, ISO_DATE_TIME).toInstant();
    GetBacklogMonitorInputDto input = new GetBacklogMonitorInputDto(
        firstDate,
        WAREHOUSE_ID,
        FBM_WMS_OUTBOUND,
        List.of(WAVING, PICKING, PACKING),
        OffsetDateTime.parse(A_DATE_MINUS_TWO_HOURS, ISO_DATE_TIME).toInstant(),
        OffsetDateTime.parse(ANOTHER_DATE, ISO_DATE_TIME).toInstant(),
        999L,
        false);

    when(requestClock.now()).thenReturn(firstDate);
    when(getBacklogMonitor.execute(input)).thenReturn(getMockedResponse());

    // WHEN
    final ResultActions result = mockMvc.perform(
        MockMvcRequestBuilders.get(format(BASE_URL, WAREHOUSE_ID) + "/monitor")
            .param("workflow", OUTBOUND)
            .param("date_from", CURRENT_DATE)
            .param("date_to", ANOTHER_DATE)
            .param("caller.id", "999")
    );

    // THEN
    result.andExpect(status().isOk());
    result.andExpect(content().json(
        getResourceAsString("get_backlog_monitor_response.json")));
  }

  @Test
  void testGetDetails() throws Exception {
    // GIVEN
    var firstDate = OffsetDateTime.parse(CURRENT_DATE, ISO_DATE_TIME).toInstant();
    GetBacklogMonitorDetailsInput input = new GetBacklogMonitorDetailsInput(
        firstDate,
        WAREHOUSE_ID,
        FBM_WMS_OUTBOUND,
        PICKING,
        OffsetDateTime.parse(A_DATE_MINUS_TWO_HOURS, ISO_DATE_TIME).toInstant(),
        OffsetDateTime.parse(ANOTHER_DATE, ISO_DATE_TIME).toInstant(),
        999L,
        false
    );

    when(requestClock.now()).thenReturn(firstDate);
    when(getBacklogMonitorDetails.execute(input)).thenReturn(getDetailsMockedResponse());

    // WHEN
    final ResultActions result = mockMvc.perform(
        MockMvcRequestBuilders.get(format(BASE_URL, WAREHOUSE_ID) + "/details")
            .param("workflow", OUTBOUND)
            .param("process", PROCESS)
            .param("date_from", CURRENT_DATE)
            .param("date_to", ANOTHER_DATE)
            .param("caller.id", "999")
    );

    // THEN
    result.andExpect(status().isOk());
    result.andExpect(content().json(
        getResourceAsString("get_backlog_details.json")));
  }

  @Test
  void testMissingWorkflowParameter() throws Exception {
    // WHEN
    final ResultActions result = mockMvc.perform(
        MockMvcRequestBuilders.get(format(BASE_URL, WAREHOUSE_ID) + "/monitor")
            .param("date_from", CURRENT_DATE)
            .param("date_to", ANOTHER_DATE)
            .param("caller.id", "999")
    );

    // THEN
    result.andExpect(status().isBadRequest());
  }

  @Test
  void testMissingCallerIdParameter() throws Exception {
    // WHEN
    final ResultActions result = mockMvc.perform(
        MockMvcRequestBuilders.get(format(BASE_URL, WAREHOUSE_ID) + "/monitor")
            .param("workflow", OUTBOUND)
            .param("date_from", CURRENT_DATE)
            .param("date_to", ANOTHER_DATE)
    );

    // THEN
    result.andExpect(status().isBadRequest());
  }

  private GetBacklogMonitorDetailsResponse getDetailsMockedResponse() {
    Instant date = OffsetDateTime.parse(CURRENT_DATE, ISO_DATE_TIME).toInstant();
    Instant anotherDate = OffsetDateTime.parse(ANOTHER_DATE, ISO_DATE_TIME).toInstant();

    return new GetBacklogMonitorDetailsResponse(
        OffsetDateTime.parse(CURRENT_DATE, ISO_DATE_TIME).toInstant(),
        List.of(
            new DetailedBacklogPhoto(
                date,
                null,
                new UnitMeasure(100, 150),
                new UnitMeasure(125, 170),
                List.of(
                    new AreaBacklogDetail(
                        "RK-H",
                        new UnitMeasure(200, 10),
                        null,
                        Collections.emptyList()
                    ),
                    new AreaBacklogDetail(
                        "RK-L",
                        new UnitMeasure(300, 15),
                        null,
                        Collections.emptyList()
                    )
                )),
            new DetailedBacklogPhoto(
                anotherDate,
                null,
                new UnitMeasure(30, 90),
                new UnitMeasure(50, 120),
                List.of(
                    new AreaBacklogDetail(
                        "RK-H",
                        new UnitMeasure(250, 20),
                        null,
                        Collections.emptyList()
                    ),
                    new AreaBacklogDetail(
                        "RK-L",
                        new UnitMeasure(350, 30),
                        null,
                        Collections.emptyList()
                    )
                ))
        ),
        new ProcessDetail(
            "waving",
            new UnitMeasure(125, 170),
            List.of(
                VariablesPhoto.builder()
                    .date(date)
                    .current(new UnitMeasure(125, 170))
                    .historical(new UnitMeasure(115, 160))
                    .build(),
                VariablesPhoto.builder()
                    .date(anotherDate)
                    .current(new UnitMeasure(50, 120))
                    .historical(new UnitMeasure(30, 70))
                    .build()
            )
        ));
  }

  private WorkflowBacklogDetail getMockedResponse() {
    Instant date = OffsetDateTime.parse(CURRENT_DATE, ISO_DATE_TIME).toInstant();
    Instant anotherDate = OffsetDateTime.parse(ANOTHER_DATE, ISO_DATE_TIME).toInstant();
    LogisticCenterConfiguration config = new LogisticCenterConfiguration(TIME_ZONE);
    final ZonedDateTime selectedDate = ZonedDateTime.ofInstant(Instant.now(), UTC);
    return new WorkflowBacklogDetail(
        getDateSelector(
            ZonedDateTime.ofInstant(Instant.now(), config.getZoneId()),
            selectedDate,
            3
        ),
        "outbound-orders",
        Instant.now(),
        List.of(
            new ProcessDetail(
                "waving",
                new UnitMeasure(100, 150),
                List.of(
                    VariablesPhoto.builder()
                        .date(date)
                        .current(new UnitMeasure(10, 30))
                        .historical(new UnitMeasure(23, null))
                        .build(),
                    VariablesPhoto.builder()
                        .date(anotherDate)
                        .current(new UnitMeasure(25, 75))
                        .historical(new UnitMeasure(44, null))
                        .build()
                )),
            new ProcessDetail(
                "picking",
                new UnitMeasure(30, 90),
                List.of(
                    VariablesPhoto.builder()
                        .date(date)
                        .current(new UnitMeasure(30, 90))
                        .historical(new UnitMeasure(60, null))
                        .build(),
                    VariablesPhoto.builder()
                        .date(anotherDate)
                        .current(new UnitMeasure(45, 120))
                        .historical(new UnitMeasure(100, null))
                        .build()
                )),
            new ProcessDetail(
                "packing",
                new UnitMeasure(200, 60),
                List.of(
                    VariablesPhoto.builder()
                        .date(date)
                        .current(new UnitMeasure(200, 60))
                        .historical(new UnitMeasure(190, null))
                        .build(),
                    VariablesPhoto.builder()
                        .date(anotherDate)
                        .current(new UnitMeasure(120, 30))
                        .historical(new UnitMeasure(115, null))
                        .build()
                ))
        )
    );
  }
}
