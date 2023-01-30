package com.mercadolibre.planning.model.me.controller;

import static com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission.OUTBOUND_PROJECTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.MonitorDataType.DEVIATION;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.TOTAL_BACKLOG;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.OUTBOUND_PLANNING;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.PACKING;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.PICKING;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.WALL_IN;
import static com.mercadolibre.planning.model.me.utils.TestUtils.USER_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID_ARTW01;
import static com.mercadolibre.planning.model.me.utils.TestUtils.getResourceAsString;
import static java.lang.String.format;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.me.controller.monitor.MonitorController;
import com.mercadolibre.planning.model.me.enums.DeviationType;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Indicator;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.ScheduleAdjustment;
import com.mercadolibre.planning.model.me.usecases.authorization.AuthorizeUser;
import com.mercadolibre.planning.model.me.usecases.authorization.dtos.AuthorizeUserDto;
import com.mercadolibre.planning.model.me.usecases.backlog.GetActiveDeviations;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogScheduled;
import com.mercadolibre.planning.model.me.usecases.backlog.entities.BacklogScheduledMetrics;
import com.mercadolibre.planning.model.me.usecases.backlog.entities.InboundBacklogMonitor;
import com.mercadolibre.planning.model.me.usecases.backlog.entities.InboundBacklogScheduled;
import com.mercadolibre.planning.model.me.usecases.backlog.entities.ProcessMetric;
import com.mercadolibre.planning.model.me.usecases.monitor.GetMonitor;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.Monitor;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.CurrentStatusData;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.DeviationData;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.deviation.DeviationActions;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.deviation.DeviationAppliedData;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.deviation.DeviationMetric;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.deviation.DeviationUnit;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.deviation.DeviationUnitDetail;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Metric;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Process;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(controllers = MonitorController.class)
class MonitorControllerTest {

  private static final String URL = "/planning/model/middleend/workflows/%s";

  private static final Instant VIEW_DATE = Instant.parse("2023-01-17T10:00:00Z");

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private GetMonitor getMonitor;

  @MockBean
  private GetBacklogScheduled getBacklogScheduled;

  @MockBean
  private GetActiveDeviations getActiveDeviations;

  @MockBean
  private RequestClock requestClock;

  @MockBean
  private AuthorizeUser authorizeUser;

  @Test
  void testGetOutboundMonitors() throws Exception {
    // GIVEN
    when(getMonitor.execute(any()))
        .thenReturn(mockCurrentStatus());

    // WHEN
    final ResultActions result = mockMvc.perform(MockMvcRequestBuilders
        .get(format(URL, FBM_WMS_OUTBOUND.getName()) + "/monitors")
        .param(WAREHOUSE_ID, WAREHOUSE_ID_ARTW01)
        .param("caller.id", String.valueOf(USER_ID))
        .param("date_from", "2019-07-27T00:00:00.000-05:00")
        .param("date_to", "2019-07-27T00:00:00.000-05:00")
    );

    // THEN
    result.andExpect(status().isOk());
    result.andExpect(content().json(getResourceAsString("get_current_status_response.json")));

    verify(authorizeUser).execute(new AuthorizeUserDto(USER_ID, List.of(OUTBOUND_PROJECTION)));
  }

  @Test
  void testGetInboundMonitors() throws Exception {
    // GIVEN
    when(requestClock.now()).thenReturn(VIEW_DATE);
    when(getBacklogScheduled.execute(
        WAREHOUSE_ID_ARTW01,
        VIEW_DATE
    ))
        .thenReturn(mockBacklogScheduled(VIEW_DATE));
    when(getActiveDeviations.execute(WAREHOUSE_ID_ARTW01, VIEW_DATE)).thenReturn(mockActiveDeviations());

    // WHEN
    final ResultActions result = mockMvc.perform(MockMvcRequestBuilders
        .get(format(URL, FBM_WMS_INBOUND.getName()) + "/monitors")
        .param("logistic_center_id", WAREHOUSE_ID_ARTW01)
        .param("caller.id", String.valueOf(USER_ID))
        .param("view_date", String.valueOf(VIEW_DATE))
    );


    // THEN
    result.andExpect(status().isOk());
    result.andExpect(content().json(getResourceAsString("get_current_status_response_inbound.json")));

    verify(authorizeUser).execute(new AuthorizeUserDto(USER_ID, List.of(OUTBOUND_PROJECTION)));
  }

    @Test
    void testGetInboundMonitorsNew() throws Exception {
        // GIVEN
        when(requestClock.now()).thenReturn(Instant.now());
        when(getBacklogScheduled.execute(
            WAREHOUSE_ID_ARTW01,
            VIEW_DATE
        ))
            .thenReturn(mockBacklogScheduled(VIEW_DATE));
        when(getActiveDeviations.execute(WAREHOUSE_ID_ARTW01, VIEW_DATE)).thenReturn(mockActiveDeviations());

        // WHEN
        final ResultActions result = mockMvc.perform(MockMvcRequestBuilders
            .get(format(URL, FBM_WMS_INBOUND.getName()) + "/monitors/new")
            .param("logistic_center_id", WAREHOUSE_ID_ARTW01)
            .param("caller.id", String.valueOf(USER_ID))
            .param("view_date", String.valueOf(VIEW_DATE))
        );


    // THEN
    result.andExpect(status().isOk());
    result.andExpect(content().json(getResourceAsString("get_current_status_response_inbound_new.json")));

    verify(authorizeUser).execute(new AuthorizeUserDto(USER_ID, List.of(OUTBOUND_PROJECTION)));
  }

  private InboundBacklogMonitor mockBacklogScheduled(Instant now) {
    return new InboundBacklogMonitor(
        now,
        List.of(
            new InboundBacklogScheduled(
                now,
                now,
                BacklogScheduledMetrics.builder()
                    .expected(Indicator.builder().shipments(500).units(15500).build())
                    .received(Indicator.builder().shipments(350).units(10850).build())
                    .deviation(Indicator.builder().shipments(2100).units(65100).build()).build(),
                BacklogScheduledMetrics.builder()
                    .expected(Indicator.builder().units(500).build())
                    .received(Indicator.builder().units(350).build())
                    .deviation(Indicator.builder().units(2100).build()).build(),
                BacklogScheduledMetrics.builder()
                    .expected(Indicator.builder().units(500).build())
                    .received(Indicator.builder().units(350).build())
                    .deviation(Indicator.builder().units(2100).build()).build()
            ),
            new InboundBacklogScheduled(
                now,
                now,
                BacklogScheduledMetrics.builder()
                    .expected(Indicator.builder().shipments(600).units(15600).build())
                    .received(Indicator.builder().shipments(450).units(10950).build())
                    .deviation(Indicator.builder().shipments(2200).units(65200).build()).build(),
                BacklogScheduledMetrics.builder()
                    .expected(Indicator.builder().units(600).build())
                    .received(Indicator.builder().units(450).build())
                    .deviation(Indicator.builder().units(2200).build()).build(),
                BacklogScheduledMetrics.builder()
                    .expected(Indicator.builder().units(400).build())
                    .received(Indicator.builder().units(450).build())
                    .deviation(Indicator.builder().units(2200).build()).build()
            )),
        new ProcessMetric(Indicator.builder().units(10340).build(), Indicator.builder().units(1030).build()),
        new ProcessMetric(Indicator.builder().units(10750).build(), Indicator.builder().units(10490).build())

    );
  }

  private List<ScheduleAdjustment> mockActiveDeviations() {
    return List.of(
        new ScheduleAdjustment(
            List.of(FBM_WMS_INBOUND),
            DeviationType.MINUTES,
            null,
            0.1,
            200,
            VIEW_DATE,
            VIEW_DATE.plus(1, ChronoUnit.DAYS)
        )
    );
  }

  private Monitor mockCurrentStatus() {
    Map<ProcessOutbound, String> processMap = new ConcurrentHashMap<>();
    processMap.put(OUTBOUND_PLANNING, "0 uds.");
    processMap.put(PICKING, "2.232 uds.");
    processMap.put(PACKING, "1.442 uds.");
    processMap.put(WALL_IN, "725 uds.");

    TreeSet<Process> processes = processMap.entrySet().stream().map(entry -> {
          final Metric metric = Metric.builder()
              .type(TOTAL_BACKLOG.getType())
              .title(TOTAL_BACKLOG.getTitle())
              .subtitle(entry.getKey().getSubtitle())
              .value(entry.getValue())
              .build();
          return Process.builder()
              .metrics(List.of(metric))
              .title(entry.getKey().getTitle())
              .build();
        }
    ).collect(Collectors.toCollection(TreeSet::new));

    return Monitor.builder()
        .title("Modelo de Priorización")
        .subtitle1("Estado Actual")
        .subtitle2("Última actualización: Today...")
        .monitorData(List.of(
            getDeviationData(),
            CurrentStatusData.builder().processes(processes).build()
        ))
        .build();
  }

  private DeviationData getDeviationData() {
    DeviationData deviationData = new DeviationData(DeviationMetric.builder()
        .deviationPercentage(Metric.builder()
            .title("% Desviación FCST / Ventas")
            .value("-5.1%")
            .icon("arrow_down")
            .build())
        .deviationUnits(DeviationUnit.builder()
            .title("Desviación en unidades")
            .value("137 uds.")
            .detail(DeviationUnitDetail.builder()
                .forecastUnits(Metric.builder()
                    .title("Cantidad Forecast")
                    .value("1042 uds.")
                    .build())
                .currentUnits(Metric.builder()
                    .title("Cantidad Real")
                    .value("905 uds.")
                    .build())
                .build())
            .build())
        .build(),
        DeviationActions.builder()
            .applyLabel("Ajustar forecast")
            .unapplyLabel("Volver al forecast")
            .appliedData(DeviationAppliedData.builder()
                .title("Se ajustó el forecast 5.80%s de 02:30 a 12:30")
                .icon("info")
                .build())
            .build());
    deviationData.setType(DEVIATION.getType());
    return deviationData;
  }

}
