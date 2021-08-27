package com.mercadolibre.planning.model.me.controller.backlog;

import com.mercadolibre.planning.model.me.entities.monitor.BacklogByDate;
import com.mercadolibre.planning.model.me.entities.monitor.ProcessDetail;
import com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure;
import com.mercadolibre.planning.model.me.entities.monitor.WorkflowBacklogDetail;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogMonitor;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorInputDto;
import com.mercadolibre.planning.model.me.utils.DateUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.getResourceAsString;
import static java.lang.String.format;
import static java.time.ZonedDateTime.parse;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BacklogMonitorController.class)
class BacklogMonitorControllerTest {
    private static final String BASE_URL = "/wms/flow/middleend/logistic_centers/%s/backlog";

    private static final String A_DATE = "2021-08-12T01:00:00Z";
    private static final String ANOTHER_DATE = "2021-08-12T04:00:00Z";

    private static final String WORKFLOW = "outbound_orders";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetBacklogMonitor getBacklogMonitor;

    @Test
    void testGetMonitor() throws Exception {
        // GIVEN
        GetBacklogMonitorInputDto input = new GetBacklogMonitorInputDto(
                WAREHOUSE_ID,
                WORKFLOW,
                parse(A_DATE, ISO_DATE_TIME),
                parse(ANOTHER_DATE, ISO_DATE_TIME),
                999L);

        when(getBacklogMonitor.execute(input)).thenReturn(getMockedResponse());

        // WHEN
        final ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders.get(format(BASE_URL, WAREHOUSE_ID) + "/monitor")
                        .param("workflow", WORKFLOW)
                        .param("date_from", A_DATE)
                        .param("date_to", ANOTHER_DATE)
                        .param("caller.id", "999")
        );

        // THEN
        result.andExpect(status().isOk());
        result.andExpect(content().json(
                getResourceAsString("get_backlog_monitor_response.json")));
    }

    @Test
    void testMissingWorkflowParameter() throws Exception {
        // WHEN
        final ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders.get(format(BASE_URL, WAREHOUSE_ID) + "/monitor")
                        .param("date_from", A_DATE)
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
                        .param("workflow", WORKFLOW)
                        .param("date_from", A_DATE)
                        .param("date_to", ANOTHER_DATE)
        );

        // THEN
        result.andExpect(status().isBadRequest());
    }

    @Test
    void testMissingDateFromParameter() throws Exception {
        // GIVEN
        when(getBacklogMonitor.execute(any())).thenReturn(getMockedResponse());

        // WHEN
        final ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders.get(format(BASE_URL, WAREHOUSE_ID) + "/monitor")
                        .param("workflow", WORKFLOW)
                        .param("date_to", ANOTHER_DATE)
                        .param("caller.id", "999")
        );

        // THEN
        result.andExpect(status().isOk());
        verify(getBacklogMonitor).execute(
                argThat(input -> checkTimeDeltaWithNow(input.getDateFrom(), 2))
        );
    }

    @Test
    void testMissingDateToParameter() throws Exception {
        // GIVEN
        when(getBacklogMonitor.execute(any())).thenReturn(getMockedResponse());

        // WHEN
        final ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders.get(format(BASE_URL, WAREHOUSE_ID) + "/monitor")
                        .param("workflow", WORKFLOW)
                        .param("date_from", A_DATE)
                        .param("caller.id", "999")
        );

        // THEN
        result.andExpect(status().isOk());
        verify(getBacklogMonitor).execute(
                argThat(input -> checkTimeDeltaWithNow(input.getDateTo(), 22))
        );
    }

    private boolean checkTimeDeltaWithNow(ZonedDateTime date, int expectedDiff) {
        ZonedDateTime now = DateUtils.getCurrentUtcDateTime();
        long diff = Math.abs(ChronoUnit.MINUTES.between(date, now));
        int expectedDiffInMinutes = expectedDiff * 60;
        return expectedDiffInMinutes - 1 < diff && diff < expectedDiffInMinutes + 1;
    }

    private WorkflowBacklogDetail getMockedResponse() {
        ZonedDateTime date = parse(A_DATE, ISO_DATE_TIME);
        ZonedDateTime anotherDate = parse(ANOTHER_DATE, ISO_DATE_TIME);

        return new WorkflowBacklogDetail("outbound-orders", List.of(
                        new ProcessDetail(
                                "waving",
                                new UnitMeasure(100, 150),
                                List.of(
                                        BacklogByDate.builder()
                                                .date(date)
                                                .current(new UnitMeasure(10, 30))
                                                .historical(23)
                                                .build(),
                                        BacklogByDate.builder()
                                                .date(anotherDate)
                                                .current(new UnitMeasure(25, 75))
                                                .historical(44)
                                                .build()
                                )),
                        new ProcessDetail(
                                "picking",
                                new UnitMeasure(30, 90),
                                List.of(
                                        BacklogByDate.builder()
                                                .date(date)
                                                .current(new UnitMeasure(30, 90))
                                                .historical(60)
                                                .build(),
                                        BacklogByDate.builder()
                                                .date(anotherDate)
                                                .current(new UnitMeasure(45, 120))
                                                .historical(100)
                                                .build()
                                )),
                        new ProcessDetail(
                                "packing",
                                new UnitMeasure(200, 60),
                                List.of(
                                        BacklogByDate.builder()
                                                .date(date)
                                                .current(new UnitMeasure(200, 60))
                                                .historical(190)
                                                .build(),
                                        BacklogByDate.builder()
                                                .date(anotherDate)
                                                .current(new UnitMeasure(120, 30))
                                                .historical(115)
                                                .build()
                                ))
                )
        );
    }
}
