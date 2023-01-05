package com.mercadolibre.planning.model.me.controller;

import com.mercadolibre.planning.model.me.controller.monitor.MonitorController;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogScheduled;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Indicator;
import com.mercadolibre.planning.model.me.usecases.authorization.AuthorizeUser;
import com.mercadolibre.planning.model.me.usecases.authorization.dtos.AuthorizeUserDto;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogScheduled;
import com.mercadolibre.planning.model.me.usecases.monitor.GetMonitor;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.Monitor;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.CurrentStatusData;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.DeviationData;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.deviation.*;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Metric;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Process;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission.OUTBOUND_PROJECTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.MonitorDataType.DEVIATION;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.TOTAL_BACKLOG;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.*;
import static com.mercadolibre.planning.model.me.utils.TestUtils.*;
import static java.lang.String.format;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MonitorController.class)
class MonitorControllerTest {

    private static final String URL = "/planning/model/middleend/workflows/%s";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetMonitor getMonitor;

    @MockBean
    private GetBacklogScheduled getBacklogScheduled;

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
                .param("warehouse_id", WAREHOUSE_ID)
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
        when(requestClock.now()).thenReturn(Instant.now());
        when(getBacklogScheduled.execute(
                        WAREHOUSE_ID,
                        requestClock.now()
        ))
                .thenReturn(mockBacklogScheduled());

        // WHEN
        final ResultActions result = mockMvc.perform(MockMvcRequestBuilders
                .get(format(URL, FBM_WMS_INBOUND.getName()) + "/monitors")
                .param("logistic_center_id", WAREHOUSE_ID)
                .param("caller.id", String.valueOf(USER_ID))
        );


        // THEN
        result.andExpect(status().isOk());
        result.andExpect(content().json(getResourceAsString("get_current_status_response_inbound.json")));

        verify(authorizeUser).execute(new AuthorizeUserDto(USER_ID, List.of(OUTBOUND_PROJECTION)));
    }

    private Map<String, BacklogScheduled> mockBacklogScheduled() {
        return Map.of("inbound", new BacklogScheduled(
            Indicator.builder().shipments(500).units(15500).build(),
            Indicator.builder().shipments(350).units(10850).build(),
            Indicator.builder().shipments(2100).units(65100).build(),
            Indicator.builder().shipments(150).units(4650).percentage(0.3).build()),
            "inbound_transfer", new BacklogScheduled(
            Indicator.builder().units(500).build(),
            Indicator.builder().units(350).build(),
            Indicator.builder().units(2100).build(),
            Indicator.builder().units(150).percentage(0.3).build()
        ));
    }

    private Monitor mockCurrentStatus() {
        Map<ProcessOutbound, String> processMap = new HashMap<>();
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
