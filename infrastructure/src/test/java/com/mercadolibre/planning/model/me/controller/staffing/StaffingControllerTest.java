package com.mercadolibre.planning.model.me.controller.staffing;

import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission.OUTBOUND_PROJECTION;
import static com.mercadolibre.planning.model.me.utils.TestUtils.AREA_MZ1;
import static com.mercadolibre.planning.model.me.utils.TestUtils.CALLER_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.CHECK_IN_PROCESS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.INBOUND_AUDIT_PROCESS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.INBOUND_WORKFLOW;
import static com.mercadolibre.planning.model.me.utils.TestUtils.OUTBOUND_WORKFLOW;
import static com.mercadolibre.planning.model.me.utils.TestUtils.PACKING_PROCESS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.PICKING_PROCESS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.RECEIVING_PROCESS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.STOCK_AUDIT_PROCESS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.STOCK_WORKFLOW;
import static com.mercadolibre.planning.model.me.utils.TestUtils.TRANSFER_WORKFLOW;
import static com.mercadolibre.planning.model.me.utils.TestUtils.USER_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID_ARTW01;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WITHDRAWALS_WORKFLOW;
import static com.mercadolibre.planning.model.me.utils.TestUtils.getResourceAsString;
import static java.lang.String.format;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.me.clients.rest.planningmodel.exception.ForecastNotFoundException;
import com.mercadolibre.planning.model.me.entities.staffing.Area;
import com.mercadolibre.planning.model.me.entities.staffing.NonSystemicWorkers;
import com.mercadolibre.planning.model.me.entities.staffing.PlannedHeadcount;
import com.mercadolibre.planning.model.me.entities.staffing.PlannedHeadcountByHour;
import com.mercadolibre.planning.model.me.entities.staffing.PlannedHeadcountByProcess;
import com.mercadolibre.planning.model.me.entities.staffing.PlannedHeadcountByWorkflow;
import com.mercadolibre.planning.model.me.entities.staffing.Process;
import com.mercadolibre.planning.model.me.entities.staffing.Staffing;
import com.mercadolibre.planning.model.me.entities.staffing.StaffingWorkflow;
import com.mercadolibre.planning.model.me.entities.staffing.Worker;
import com.mercadolibre.planning.model.me.usecases.authorization.AuthorizeUser;
import com.mercadolibre.planning.model.me.usecases.authorization.dtos.AuthorizeUserDto;
import com.mercadolibre.planning.model.me.usecases.staffing.GetPlannedHeadcount;
import com.mercadolibre.planning.model.me.usecases.staffing.GetStaffing;
import com.mercadolibre.planning.model.me.usecases.staffing.dtos.GetPlannedHeadcountInput;
import com.mercadolibre.planning.model.me.usecases.staffing.dtos.GetStaffingInput;
import java.util.Collections;
import java.util.List;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * This class tests the behavior of "/current" and "/plan" endpoints for the StaffingController.
 */
@WebMvcTest(controllers = StaffingController.class)
public class StaffingControllerTest {

  private static final String URL = "/wms/flow/middleend/logistic_center_id/%s/staffing";

  private static final String CURRENT_URL = "/current";

  private static final String PLAN_URL = "/plan";

  private static final List<String> AREA_PROCESSES =
      List.of(PICKING_PROCESS, INBOUND_AUDIT_PROCESS);

  private static final int STAFFING_NET_PRODUCTIVITY = 34;

  private static final int STAFFING_TOTAL_WORKERS = 550;

  private static final int STAFFING_PLANNED_WORKERS = 320;

  private static final int WORKFLOW_NET_PRODUCTIVITY = 60;

  private static final int WORKFLOW_TOTAL_WORKERS = 100;

  private static final int WORKFLOW_NS_WORKERS = 10;

  private static final int WORKFLOW_NS_WORKERS_PROCESS = 2;

  private static final int PROCESS_NET_PRODUCTIVITY = 40;

  private static final int PROCESS_TARGET_PRODUCTIVITY = 50;

  private static final int PROCESS_IDLE_WORKERS = 10;

  private static final int PROCESS_BUSY_WORKERS = 30;

  private static final int PROCESS_NON_SYS_WORKERS = 5;

  private static final int PROCESS_THROUGHPUT = 1200;

  private static final int AREA_NET_PRODUCTIVITY = 40;

  private static final int AREA_IDLE_WORKERS = 10;

  private static final int AREA_BUSY_WORKERS = 30;

  private static final int HEADCOUNT_OUTBOUND_WORKFLOW_WORKERS_1 = 17;

  private static final int HEADCOUNT_PICKING_PROCESS_WORKERS_1 = 10;

  private static final int HEADCOUNT_PACKING_PROCESS_WORKERS_1 = 5;

  private static final int HEADCOUNT_PICKING_PROCESS_THROUGHPUT_1 = 100;

  private static final int HEADCOUNT_PACKING_PROCESS_THROUGHPUT_1 = 58;

  private static final int HEADCOUNT_OUTBOUND_WORKFLOW_WORKERS_2 = 26;

  private static final int HEADCOUNT_PICKING_PROCESS_WORKERS_2 = 15;

  private static final int HEADCOUNT_PACKING_PROCESS_WORKERS_2 = 8;

  private static final int HEADCOUNT_PICKING_PROCESS_THROUGHPUT_2 = 120;

  private static final int HEADCOUNT_PACKING_PROCESS_THROUGHPUT_2 = 82;

  private static final String HEADCOUNT_HOUR_1 = "12:00";

  private static final String HEADCOUNT_HOUR_2 = "13:00";

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private AuthorizeUser authorizeUser;

  @MockBean
  private GetStaffing getStaffing;

  @MockBean
  private GetPlannedHeadcount getPlannedHeadcount;

  @Test
  public void testGetStaffing() throws Exception {
    // GIVEN
    when(getStaffing.execute(new GetStaffingInput(WAREHOUSE_ID_ARTW01))).thenReturn(mockStaffing());

    // WHEN
    final ResultActions result =
        mockMvc.perform(
            MockMvcRequestBuilders.get(format(URL, WAREHOUSE_ID_ARTW01) + CURRENT_URL)
                .param(CALLER_ID, String.valueOf(USER_ID))
                .contentType(APPLICATION_JSON));

    // THEN
    result.andExpect(status().isOk());
    result.andExpect(content().json(getResourceAsString("get_staffing_response.json")));
    verify(authorizeUser).execute(new AuthorizeUserDto(USER_ID, List.of(OUTBOUND_PROJECTION)));
  }

  @Test
  public void testGetPlannedHeadcountOk() throws Exception {
    // GIVEN
    when(getPlannedHeadcount.execute(new GetPlannedHeadcountInput(WAREHOUSE_ID_ARTW01)))
        .thenReturn(mockPlannedHeadcount());

    // WHEN
    final ResultActions result =
        mockMvc.perform(
            MockMvcRequestBuilders.get(format(URL, WAREHOUSE_ID_ARTW01) + PLAN_URL)
                .param(CALLER_ID, String.valueOf(USER_ID))
                .contentType(APPLICATION_JSON));

    // THEN
    result.andExpect(status().isOk());
    result.andExpect(content().json(getResourceAsString("get_planned_headcount_response.json")));

    verify(authorizeUser).execute(new AuthorizeUserDto(USER_ID, List.of(OUTBOUND_PROJECTION)));
  }

  @Test
  public void testGetPlannedHeadcountError() throws Exception {
    // GIVEN
    when(getPlannedHeadcount.execute(new GetPlannedHeadcountInput(WAREHOUSE_ID_ARTW01)))
        .thenThrow(ForecastNotFoundException.class);

    // WHEN
    final ResultActions result =
        mockMvc.perform(
            MockMvcRequestBuilders.get(format(URL, WAREHOUSE_ID_ARTW01) + PLAN_URL)
                .param(CALLER_ID, String.valueOf(USER_ID))
                .contentType(APPLICATION_JSON));

    // THEN
    result.andExpect(status().isNotFound());
    result.andExpect(
        content()
            .json(
                new JSONObject()
                    .put("status", "NOT_FOUND")
                    .put("error", "forecast_not_found")
                    .toString()));

    verify(authorizeUser).execute(new AuthorizeUserDto(USER_ID, List.of(OUTBOUND_PROJECTION)));
  }

  private Staffing mockStaffing() {
    final StaffingWorkflow mockedOutboundWorkflow =
        mockStaffingWorkflow(
            OUTBOUND_WORKFLOW,
            List.of(
                mockProcess(PICKING_PROCESS, OUTBOUND_WORKFLOW),
                mockProcess(PACKING_PROCESS, OUTBOUND_WORKFLOW)));

    final StaffingWorkflow mockedInboundWorkflow =
        mockStaffingWorkflow(
            INBOUND_WORKFLOW,
            List.of(
                mockProcess(RECEIVING_PROCESS, INBOUND_WORKFLOW),
                mockProcess(CHECK_IN_PROCESS, INBOUND_WORKFLOW)));
    final StaffingWorkflow mockedWithdrawalsWorkflow =
        mockStaffingWorkflow(
            WITHDRAWALS_WORKFLOW,
            List.of(
                mockProcess(PICKING_PROCESS, WITHDRAWALS_WORKFLOW),
                mockProcess(PACKING_PROCESS, WITHDRAWALS_WORKFLOW)));
    final StaffingWorkflow mockedStockWorkflow =
        mockStaffingWorkflow(
            STOCK_WORKFLOW,
            List.of(
                mockProcess(STOCK_AUDIT_PROCESS, STOCK_WORKFLOW),
                mockProcess(INBOUND_AUDIT_PROCESS, STOCK_WORKFLOW)));
    final StaffingWorkflow mockedTransferWorkflow =
        mockStaffingWorkflow(
            TRANSFER_WORKFLOW, List.of(mockProcess(PICKING_PROCESS, TRANSFER_WORKFLOW)));

    return Staffing.builder()
        .globalNetProductivity(STAFFING_NET_PRODUCTIVITY)
        .totalWorkers(STAFFING_TOTAL_WORKERS)
        .plannedWorkers(STAFFING_PLANNED_WORKERS)
        .workflows(
            List.of(
                mockedOutboundWorkflow,
                mockedInboundWorkflow,
                mockedWithdrawalsWorkflow,
                mockedStockWorkflow,
                mockedTransferWorkflow))
        .build();
  }

  private StaffingWorkflow mockStaffingWorkflow(
      final String workflow, final List<Process> processes) {
    return StaffingWorkflow.builder()
        .workflow(workflow)
        .totalWorkers(WORKFLOW_TOTAL_WORKERS)
        .globalNetProductivity(WORKFLOW_NET_PRODUCTIVITY)
        .nonSystemicWorkers(
            NonSystemicWorkers.builder()
                .total(WORKFLOW_NS_WORKERS)
                .cross(WORKFLOW_NS_WORKERS - WORKFLOW_NS_WORKERS_PROCESS)
                .subProcesses(WORKFLOW_NS_WORKERS_PROCESS)
                .build())
        .processes(processes)
        .build();
  }

  private Process mockProcess(final String process, final String workflow) {
    Integer FORECAST_PLANNED_WORKERS = null;
    Integer delta = null;
    if ((workflow.equals(INBOUND_WORKFLOW) || workflow.equals(OUTBOUND_WORKFLOW))
        && (process.equals(CHECK_IN_PROCESS)
        || process.equals(PICKING_PROCESS)
        || process.equals(PACKING_PROCESS))) {
      FORECAST_PLANNED_WORKERS = 15;
      delta = (PROCESS_BUSY_WORKERS + PROCESS_IDLE_WORKERS) - FORECAST_PLANNED_WORKERS;
    }

    return Process.builder()
        .process(process)
        .netProductivity(PROCESS_NET_PRODUCTIVITY)
        .workers(
            new Worker(
                PROCESS_IDLE_WORKERS,
                PROCESS_BUSY_WORKERS,
                PROCESS_NON_SYS_WORKERS,
                FORECAST_PLANNED_WORKERS,
                delta))
        .targetProductivity(PROCESS_TARGET_PRODUCTIVITY)
        .throughput(PROCESS_THROUGHPUT)
        .areas(
            AREA_PROCESSES.contains(process)
                ? List.of(
                new Area(
                    AREA_MZ1,
                    AREA_NET_PRODUCTIVITY,
                    new Worker(AREA_IDLE_WORKERS, AREA_BUSY_WORKERS)))
                : Collections.emptyList())
        .build();
  }

  private PlannedHeadcount mockPlannedHeadcount() {
    return new PlannedHeadcount(
        List.of(
            new PlannedHeadcountByHour(
                HEADCOUNT_HOUR_1,
                List.of(
                    new PlannedHeadcountByWorkflow(
                        OUTBOUND_WORKFLOW,
                        HEADCOUNT_OUTBOUND_WORKFLOW_WORKERS_1,
                        List.of(
                            new PlannedHeadcountByProcess(
                                PACKING.getName(),
                                HEADCOUNT_PACKING_PROCESS_WORKERS_1,
                                HEADCOUNT_PACKING_PROCESS_THROUGHPUT_1),
                            new PlannedHeadcountByProcess(
                                PICKING.getName(),
                                HEADCOUNT_PICKING_PROCESS_WORKERS_1,
                                HEADCOUNT_PICKING_PROCESS_THROUGHPUT_1))))),
            new PlannedHeadcountByHour(
                HEADCOUNT_HOUR_2,
                List.of(
                    new PlannedHeadcountByWorkflow(
                        OUTBOUND_WORKFLOW,
                        HEADCOUNT_OUTBOUND_WORKFLOW_WORKERS_2,
                        List.of(
                            new PlannedHeadcountByProcess(
                                PACKING.getName(),
                                HEADCOUNT_PACKING_PROCESS_WORKERS_2,
                                HEADCOUNT_PACKING_PROCESS_THROUGHPUT_2),
                            new PlannedHeadcountByProcess(
                                PICKING.getName(),
                                HEADCOUNT_PICKING_PROCESS_WORKERS_2,
                                HEADCOUNT_PICKING_PROCESS_THROUGHPUT_2)))))));
  }
}
