package com.mercadolibre.planning.model.me.controller;

import com.mercadolibre.planning.model.me.controller.monitor.MonitorController;
import com.mercadolibre.planning.model.me.usecases.authorization.AuthorizeUser;
import com.mercadolibre.planning.model.me.usecases.authorization.dtos.AuthorizeUserDto;
import com.mercadolibre.planning.model.me.usecases.currentstatus.GetMonitor;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.Monitor;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.CurrentStatusData;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.DeviationData;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.Metric;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.Process;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission.OUTBOUND_PROJECTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.MetricType.BACKLOG;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo.OUTBOUND_PLANNING;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo.PACKING;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo.PICKING;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo.WALL_IN;
import static com.mercadolibre.planning.model.me.utils.TestUtils.USER_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.getResourceAsString;
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
    private AuthorizeUser authorizeUser;

    @Test
    void testSaveSimulation() throws Exception {
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

    private Monitor mockCurrentStatus() {
        Map<ProcessInfo, String> processMap = new HashMap<>();
        processMap.put(OUTBOUND_PLANNING, "0 uds.");
        processMap.put(PICKING, "2.232 uds.");
        processMap.put(PACKING, "1.442 uds.");
        processMap.put(WALL_IN, "725 uds.");

        List<Process> processes = processMap.entrySet().stream().map(entry -> {
                    final Metric metric = Metric.builder()
                            .type(BACKLOG.getType())
                            .title(BACKLOG.getTitle())
                            .subtitle(entry.getKey().getSubtitle())
                            .value(entry.getValue())
                            .build();
                    return Process.builder()
                            .metrics(List.of(metric))
                            .title(entry.getKey().getTitle())
                            .build();
                }
        ).collect(Collectors.toList());

        return Monitor.builder()
                .title("Modelo de Priorización")
                .subtitle1("Estado Actual")
                .subtitle2("Última actualización: Today...")
                .monitorData(List.of(
                        new DeviationData(),
                        CurrentStatusData.builder().processes(processes).build()
                ))
                .build();
    }

}