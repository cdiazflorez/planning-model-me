package com.mercadolibre.planning.model.me.usecases.staffing;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityFilters.PROCESSING_TYPE;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType.ACTIVE_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.AREA_MZ1;
import static com.mercadolibre.planning.model.me.utils.TestUtils.AREA_RKL;
import static com.mercadolibre.planning.model.me.utils.TestUtils.BATCH_SORTER_PROCESS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.CHECK_IN_PROCESS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.CHECK_IN_SYS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.CYCLE_COUNT_PROCESS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.INBOUND_AUDIT_PROCESS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.INBOUND_IDLE_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.INBOUND_SYS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.INBOUND_WORKFLOW;
import static com.mercadolibre.planning.model.me.utils.TestUtils.OUTBOUND_IDLE_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.OUTBOUND_NS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.OUTBOUND_NS_WORKERS_PROCESS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.OUTBOUND_PACKING_NON_SYS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.OUTBOUND_PACKING_SYS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.OUTBOUND_PACKING_WALL_IDLE_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.OUTBOUND_PACKING_WALL_NON_SYS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.OUTBOUND_PACKING_WALL_SYS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.OUTBOUND_PICKING_IDLE_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.OUTBOUND_PICKING_MZ1_IDLE_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.OUTBOUND_PICKING_MZ1_SYS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.OUTBOUND_PICKING_NON_SYS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.OUTBOUND_PICKING_RKL_IDLE_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.OUTBOUND_PICKING_RKL_SYS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.OUTBOUND_PICKING_SYS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.OUTBOUND_SYS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.OUTBOUND_WORKFLOW;
import static com.mercadolibre.planning.model.me.utils.TestUtils.PACKING_PROCESS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.PACKING_WALL_PROCESS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.PICKING_PROCESS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.PUT_AWAY_MZ1_SYS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.PUT_AWAY_PROCESS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.PUT_AWAY_RKL_SYS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.PUT_AWAY_SYS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.RECEIVING_PROCESS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.RECEIVING_SYS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.STOCK_AUDIT_PROCESS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.STOCK_CYCLE_COUNT_IDLE_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.STOCK_CYCLE_COUNT_MZ1_IDLE_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.STOCK_CYCLE_COUNT_MZ1_SYS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.STOCK_CYCLE_COUNT_NON_SYS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.STOCK_CYCLE_COUNT_SYS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.STOCK_IDLE_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.STOCK_INBOUND_AUDIT_RKL_SYS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.STOCK_INBOUND_AUDIT_SYS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.STOCK_NS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.STOCK_NS_WORKERS_PROCESS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.STOCK_STOCK_AUDIT_IDLE_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.STOCK_STOCK_AUDIT_SYS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.STOCK_SYS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.STOCK_WORKFLOW;
import static com.mercadolibre.planning.model.me.utils.TestUtils.TRANSFER_IDLE_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.TRANSFER_NS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.TRANSFER_PICKING_IDLE_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.TRANSFER_PICKING_RKL_IDLE_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.TRANSFER_PICKING_RKL_SYS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.TRANSFER_PICKING_SYS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.TRANSFER_SYS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.TRANSFER_WORKFLOW;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WALL_IN_PROCESS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WITHDRAWALS_IDLE_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WITHDRAWALS_NS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WITHDRAWALS_NS_WORKERS_PROCESS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WITHDRAWALS_PACKING_IDLE_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WITHDRAWALS_PACKING_NON_SYS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WITHDRAWALS_PACKING_SYS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WITHDRAWALS_PICKING_IDLE_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WITHDRAWALS_PICKING_NON_SYS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WITHDRAWALS_PICKING_RKL_IDLE_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WITHDRAWALS_PICKING_RKL_SYS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WITHDRAWALS_PICKING_SYS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WITHDRAWALS_SYS_WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WITHDRAWALS_WORKFLOW;
import static com.mercadolibre.planning.model.me.utils.TestUtils.inboundProcesses;
import static com.mercadolibre.planning.model.me.utils.TestUtils.outboundProcesses;
import static com.mercadolibre.planning.model.me.utils.TestUtils.stockProcesses;
import static com.mercadolibre.planning.model.me.utils.TestUtils.transferProcesses;
import static com.mercadolibre.planning.model.me.utils.TestUtils.withdrawalsProcesses;
import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.entities.staffing.NonSystemicWorkers;
import com.mercadolibre.planning.model.me.entities.staffing.Process;
import com.mercadolibre.planning.model.me.entities.staffing.Staffing;
import com.mercadolibre.planning.model.me.entities.staffing.StaffingWorkflow;
import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityFilters;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SearchTrajectoriesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.gateways.staffing.StaffingGateway;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.StaffingResponse;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.StaffingWorkflowResponse;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.WorkflowTotals;
import com.mercadolibre.planning.model.me.usecases.staffing.dtos.GetStaffingInput;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.exceptions.base.MockitoException;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetStaffingTest {

  private static final int TOTAL_WORKFLOWS = 5;

  private static final int TOTAL_WORKERS = 223;

  private static final int TOTAL_INBOUND_PROCESSES = 3;

  private static final int TOTAL_INBOUND_WORKERS = 20;

  private static final int INDEX_WALL_IN_PROCESS = 2;

  private static final int INDEX_PACKING_PROCESS = 3;

  private static final int INDEX_PACKING_WALL_PROCESS = 4;

  private static final Integer EXPECTED_RECEIVING_NET_PRODUCTIVITY = 25;

  private static final Integer EXPECTED_RECEIVING_THROUGHPUT = 1;

  private static final Integer EXPECTED_CHECK_IN_NET_PRODUCTIVITY = 35;

  private static final Integer EXPECTED_CHECK_IN_THROUGHPUT = 11;

  private static final Integer EXPECTED_PUT_AWAY_NET_PRODUCTIVITY = 15;

  private static final Integer EXPECTED_PUT_AWAY_THROUGHPUT = 21;

  private static final int TOTAL_PUT_AWAY_AREAS = 2;

  private static final Integer EXPECTED_PUT_AWAY_MZ1_NET_PRODUCTIVITY = 32;

  private static final Integer EXPECTED_PUT_AWAY_RKL_NET_PRODUCTIVITY = 22;

  private static final int TOTAL_OUTBOUND_PROCESSES = 5;

  private static final int TOTAL_OUTBOUND_WORKERS = 102;

  private static final Integer EXPECTED_OUTBOUND_PICKING_NET_PRODUCTIVITY = 45;

  private static final Integer EXPECTED_OUTBOUND_PICKING_THROUGHPUT = 2700;

  private static final int TOTAL_OUTBOUND_PICKING_AREAS = 2;

  private static final Integer EXPECTED_OUTBOUND_PICKING_MZ1_NET_PRODUCTIVITY = 60;

  private static final Integer EXPECTED_OUTBOUND_PICKING_RKL_NET_PRODUCTIVITY = 75;

  private static final Integer EXPECTED_OUTBOUND_PACKING_NET_PRODUCTIVITY = 34;

  private static final Integer EXPECTED_OUTBOUND_PACKING_THROUGHPUT = 1350;

  private static final Integer EXPECTED_OUTBOUND_PACKING_WALL_NET_PRODUCTIVITY = 32;

  private static final Integer EXPECTED_OUTBOUND_PACKING_WALL_THROUGHPUT = 11;

  private static final int TOTAL_WITHDRAWALS_PROCESSES = 2;

  private static final int TOTAL_WITHDRAWALS_WORKERS = 54;

  private static final Integer EXPECTED_WITHDRAWALS_PICKING_NET_PRODUCTIVITY = 71;

  private static final Integer EXPECTED_WITHDRAWALS_PICKING_THROUGHPUT = 270;

  private static final int TOTAL_WITHDRAWALS_PICKING_AREAS = 1;

  private static final Integer EXPECTED_WITHDRAWALS_PICKING_RKL_NET_PRODUCTIVITY = 71;

  private static final Integer EXPECTED_WITHDRAWALS_PACKING_NET_PRODUCTIVITY = 54;

  private static final Integer EXPECTED_WITHDRAWALS_PACKING_THROUGHPUT = 350;

  private static final int TOTAL_TRANSFER_PROCESSES = 1;

  private static final int TOTAL_TRANSFER_WORKERS = 27;

  private static final Integer EXPECTED_TRANSFER_PICKING_NET_PRODUCTIVITY = 35;

  private static final Integer EXPECTED_TRANSFER_PICKING_THROUGHPUT = 135;

  private static final int TOTAL_TRANSFER_PICKING_AREAS = 1;

  private static final Integer EXPECTED_TRANSFER_PICKING_RKL_NET_PRODUCTIVITY = 35;

  private static final int TOTAL_STOCK_PROCESSES = 3;

  private static final int TOTAL_STOCK_WORKERS = 20;

  private static final Integer EXPECTED_STOCK_CYCLE_COUNT_EFF_PRODUCTIVITY = 565;

  private static final Integer EXPECTED_STOCK_STOCK_CYCLE_COUNT_THROUGHPUT = 1585;

  private static final int TOTAL_STOCK_CYCLE_COUNT_AREAS = 1;

  private static final Integer EXPECTED_STOCK_CYCLE_COUNT_MZ1_EFF_PRODUCTIVITY = 565;

  private static final Integer EXPECTED_STOCK_INBOUND_AUDIT_EFF_PRODUCTIVITY = 1200;

  private static final Integer EXPECTED_STOCK_STOCK_INBOUND_AUDIT_THROUGHPUT = 225;

  private static final int TOTAL_STOCK_INBOUND_AUDIT_AREAS = 1;

  private static final Integer EXPECTED_STOCK_INBOUND_AUDIT_RKL_EFF_PRODUCTIVITY = 1200;

  private static final Integer EXPECTED_STOCK_STOCK_AUDIT_NET_PRODUCTIVITY = 594;

  private static final Integer EXPECTED_STOCK_STOCK_AUDIT_THROUGHPUT = 2615;

  private static final Integer FORECAST_PICKING = 30;

  private static final Integer FORECAST_WALL_IN = 20;

  private static final Integer FORECAST_WAVING = 40;

  private static final Integer FORECAST_PACKING = 35;

  private static final Integer FORECAST_HEADCOUNT_PICKING = 4;

  private static final Integer FORECAST_HEADCOUNT_PACKING_WALL = 0;

  private static final Integer FORECAST_HEADCOUNT_PACKING = 10;

  private static final Integer FORECAST_HEADCOUNT_BATCH_SORTER = 1;

  private static final Integer FORECAST_HEADCOUNT_CHECK_IN = 4;

  private static final Integer FORECAST_HEADCOUNT_PUT_AWAY = 7;

  private static final Integer SIMULATION_HEADCOUNT_PUT_AWAY = 10;

  private static final Integer EXPECTED_DELTA_PUT_AWAY = 0;

  private static final Integer EXPECTED_DELTA_CHECK_IN = 3;

  private static final Integer EXPECTED_DELTA_PICKING = 22;

  private static final Integer EXPECTED_DELTA_PACKING = 6;

  private static final Integer EXPECTED_DELTA_PACKING_WALL = 40;

  @InjectMocks
  private GetStaffing useCase;

  @Mock
  private PlanningModelGateway planningModelGateway;

  @Mock
  private StaffingGateway staffingGateway;

  @Test
  void testExecute() {
    // GIVEN
    final GetStaffingInput input = new GetStaffingInput(WAREHOUSE_ID);

    when(staffingGateway.getStaffing(WAREHOUSE_ID)).thenReturn(mockStaffingResponse());

    givenSearchTrajectoriesWithProductivityForecast();
    givenSearchTrajectoriesWithoutHeadcountForecast();

    // WHEN
    final Staffing staffing = useCase.execute(input);

    // THEN
    final StaffingWorkflow outbound =
        staffing.getWorkflows().stream()
            .filter(w -> w.getWorkflow().equals(OUTBOUND_WORKFLOW))
            .findFirst()
            .orElseThrow();

    final StaffingWorkflow inbound =
        staffing.getWorkflows().stream()
            .filter(w -> w.getWorkflow().equals(INBOUND_WORKFLOW))
            .findFirst()
            .orElseThrow();

    final StaffingWorkflow withdrawals =
        staffing.getWorkflows().stream()
            .filter(w -> w.getWorkflow().equals(WITHDRAWALS_WORKFLOW))
            .findFirst()
            .orElseThrow();

    final StaffingWorkflow stock =
        staffing.getWorkflows().stream()
            .filter(w -> w.getWorkflow().equals(STOCK_WORKFLOW))
            .findFirst()
            .orElseThrow();

    final Process putAway = inbound.getProcesses().get(2);
    final Process picking = outbound.getProcesses().get(0);
    final Process pickingWithdrawals = withdrawals.getProcesses().get(0);
    final Process cycleCount = stock.getProcesses().get(0);
    final Process inboundAudit = stock.getProcesses().get(1);
    final var putAwayAreas = putAway.getAreas();
    final var pickingAreas = picking.getAreas();
    final var pickingWithdrawalsAreas = pickingWithdrawals.getAreas();
    final var cycleCountAreas = cycleCount.getAreas();
    final var inboundAuditAreas = inboundAudit.getAreas();

    assertEquals(TOTAL_WORKERS, staffing.getTotalWorkers());
    assertEquals(TOTAL_WORKFLOWS, staffing.getWorkflows().size());

    assertEqualsWorkflow(
        inbound,
        INBOUND_WORKFLOW,
        TOTAL_INBOUND_WORKERS,
        NonSystemicWorkers.builder().total(0).cross(0).subProcesses(0).build(),
        TOTAL_INBOUND_PROCESSES);

    assertEqualsProcess(
        inbound.getProcesses().get(0),
        RECEIVING_PROCESS,
        EXPECTED_RECEIVING_NET_PRODUCTIVITY,
        null,
        0,
        RECEIVING_SYS_WORKERS,
        0,
        EXPECTED_RECEIVING_THROUGHPUT,
        0);

    assertEqualsProcess(
        inbound.getProcesses().get(1),
        CHECK_IN_PROCESS,
        EXPECTED_CHECK_IN_NET_PRODUCTIVITY,
        null,
        1,
        CHECK_IN_SYS_WORKERS,
        0,
        EXPECTED_CHECK_IN_THROUGHPUT,
        0);

    assertEqualsProcess(
        putAway,
        PUT_AWAY_PROCESS,
        EXPECTED_PUT_AWAY_NET_PRODUCTIVITY,
        null,
        1,
        PUT_AWAY_SYS_WORKERS,
        0,
        EXPECTED_PUT_AWAY_THROUGHPUT,
        TOTAL_PUT_AWAY_AREAS);

    assertEquals(AREA_MZ1, putAwayAreas.get(0).getArea());
    assertEquals(0, putAwayAreas.get(0).getWorkers().getIdle());
    assertEquals(PUT_AWAY_MZ1_SYS_WORKERS, putAwayAreas.get(0).getWorkers().getBusy());
    assertEquals(EXPECTED_PUT_AWAY_MZ1_NET_PRODUCTIVITY, putAwayAreas.get(0).getNetProductivity());

    assertEquals(AREA_RKL, putAwayAreas.get(1).getArea());
    assertEquals(1, putAwayAreas.get(1).getWorkers().getIdle());
    assertEquals(PUT_AWAY_RKL_SYS_WORKERS, putAwayAreas.get(1).getWorkers().getBusy());
    assertEquals(EXPECTED_PUT_AWAY_RKL_NET_PRODUCTIVITY, putAwayAreas.get(1).getNetProductivity());

    assertEqualsWorkflow(
        outbound,
        OUTBOUND_WORKFLOW,
        TOTAL_OUTBOUND_WORKERS,
        NonSystemicWorkers.builder()
            .total(OUTBOUND_NS_WORKERS)
            .cross(OUTBOUND_NS_WORKERS - OUTBOUND_NS_WORKERS_PROCESS)
            .subProcesses(OUTBOUND_NS_WORKERS_PROCESS)
            .build(),
        TOTAL_OUTBOUND_PROCESSES);

    assertEqualsProcess(
        picking,
        PICKING_PROCESS,
        EXPECTED_OUTBOUND_PICKING_NET_PRODUCTIVITY,
        FORECAST_PICKING,
        OUTBOUND_PICKING_IDLE_WORKERS,
        OUTBOUND_PICKING_SYS_WORKERS,
        OUTBOUND_PICKING_NON_SYS_WORKERS,
        EXPECTED_OUTBOUND_PICKING_THROUGHPUT,
        TOTAL_OUTBOUND_PICKING_AREAS);

    assertEquals(AREA_MZ1, pickingAreas.get(0).getArea());
    assertEquals(OUTBOUND_PICKING_MZ1_IDLE_WORKERS, pickingAreas.get(0).getWorkers().getIdle());
    assertEquals(OUTBOUND_PICKING_MZ1_SYS_WORKERS, pickingAreas.get(0).getWorkers().getBusy());
    assertEquals(
        EXPECTED_OUTBOUND_PICKING_MZ1_NET_PRODUCTIVITY, pickingAreas.get(0).getNetProductivity());

    assertEquals(AREA_RKL, pickingAreas.get(1).getArea());
    assertEquals(OUTBOUND_PICKING_RKL_IDLE_WORKERS, pickingAreas.get(1).getWorkers().getIdle());
    assertEquals(OUTBOUND_PICKING_RKL_SYS_WORKERS, pickingAreas.get(1).getWorkers().getBusy());
    assertEquals(
        EXPECTED_OUTBOUND_PICKING_RKL_NET_PRODUCTIVITY, pickingAreas.get(1).getNetProductivity());

    assertNullProcess(outbound.getProcesses().get(1), BATCH_SORTER_PROCESS);

    assertEqualsProcess(
        outbound.getProcesses().get(INDEX_WALL_IN_PROCESS),
        WALL_IN_PROCESS,
        0,
        FORECAST_WALL_IN,
        0,
        0,
        0,
        0,
        0);

    assertEqualsProcess(
        outbound.getProcesses().get(INDEX_PACKING_PROCESS),
        PACKING_PROCESS,
        EXPECTED_OUTBOUND_PACKING_NET_PRODUCTIVITY,
        FORECAST_PACKING,
        1,
        OUTBOUND_PACKING_SYS_WORKERS,
        OUTBOUND_PACKING_NON_SYS_WORKERS,
        EXPECTED_OUTBOUND_PACKING_THROUGHPUT,
        0);

    assertEqualsProcess(
        outbound.getProcesses().get(INDEX_PACKING_WALL_PROCESS),
        PACKING_WALL_PROCESS,
        EXPECTED_OUTBOUND_PACKING_WALL_NET_PRODUCTIVITY,
        null,
        OUTBOUND_PACKING_WALL_IDLE_WORKERS,
        OUTBOUND_PACKING_WALL_SYS_WORKERS,
        OUTBOUND_PACKING_WALL_NON_SYS_WORKERS,
        EXPECTED_OUTBOUND_PACKING_WALL_THROUGHPUT,
        0);

    assertEqualsWorkflow(
        withdrawals,
        WITHDRAWALS_WORKFLOW,
        TOTAL_WITHDRAWALS_WORKERS,
        NonSystemicWorkers.builder()
            .total(WITHDRAWALS_NS_WORKERS)
            .cross(WITHDRAWALS_NS_WORKERS - WITHDRAWALS_NS_WORKERS_PROCESS)
            .subProcesses(WITHDRAWALS_NS_WORKERS_PROCESS)
            .build(),
        TOTAL_WITHDRAWALS_PROCESSES);

    assertEqualsProcess(
        pickingWithdrawals,
        PICKING_PROCESS,
        EXPECTED_WITHDRAWALS_PICKING_NET_PRODUCTIVITY,
        null,
        WITHDRAWALS_PICKING_IDLE_WORKERS,
        WITHDRAWALS_PICKING_SYS_WORKERS,
        WITHDRAWALS_PICKING_NON_SYS_WORKERS,
        EXPECTED_WITHDRAWALS_PICKING_THROUGHPUT,
        TOTAL_WITHDRAWALS_PICKING_AREAS);

    assertEquals(AREA_RKL, pickingWithdrawalsAreas.get(0).getArea());

    assertEquals(
        WITHDRAWALS_PICKING_RKL_IDLE_WORKERS,
        pickingWithdrawalsAreas.get(0).getWorkers().getIdle());
    assertEquals(
        WITHDRAWALS_PICKING_RKL_SYS_WORKERS, pickingWithdrawalsAreas.get(0).getWorkers().getBusy());
    assertEquals(
        EXPECTED_WITHDRAWALS_PICKING_RKL_NET_PRODUCTIVITY,
        pickingWithdrawalsAreas.get(0).getNetProductivity());

    assertEqualsProcess(
        withdrawals.getProcesses().get(1),
        PACKING_PROCESS,
        EXPECTED_WITHDRAWALS_PACKING_NET_PRODUCTIVITY,
        null,
        WITHDRAWALS_PACKING_IDLE_WORKERS,
        WITHDRAWALS_PACKING_SYS_WORKERS,
        WITHDRAWALS_PACKING_NON_SYS_WORKERS,
        EXPECTED_WITHDRAWALS_PACKING_THROUGHPUT,
        0);

    assertEqualsWorkflow(
        stock,
        STOCK_WORKFLOW,
        TOTAL_STOCK_WORKERS,
        NonSystemicWorkers.builder()
            .total(STOCK_NS_WORKERS)
            .cross(STOCK_NS_WORKERS - STOCK_NS_WORKERS_PROCESS)
            .subProcesses(STOCK_NS_WORKERS_PROCESS)
            .build(),
        TOTAL_STOCK_PROCESSES);

    assertEqualsProcess(
        cycleCount,
        CYCLE_COUNT_PROCESS,
        EXPECTED_STOCK_CYCLE_COUNT_EFF_PRODUCTIVITY,
        null,
        STOCK_CYCLE_COUNT_IDLE_WORKERS,
        STOCK_CYCLE_COUNT_SYS_WORKERS,
        STOCK_CYCLE_COUNT_NON_SYS_WORKERS,
        EXPECTED_STOCK_STOCK_CYCLE_COUNT_THROUGHPUT,
        TOTAL_STOCK_CYCLE_COUNT_AREAS);

    assertEquals(AREA_MZ1, cycleCountAreas.get(0).getArea());
    assertEquals(STOCK_CYCLE_COUNT_MZ1_IDLE_WORKERS, cycleCountAreas.get(0).getWorkers().getIdle());
    assertEquals(STOCK_CYCLE_COUNT_MZ1_SYS_WORKERS, cycleCountAreas.get(0).getWorkers().getBusy());
    assertEquals(
        EXPECTED_STOCK_CYCLE_COUNT_MZ1_EFF_PRODUCTIVITY,
        cycleCountAreas.get(0).getNetProductivity());

    assertEqualsProcess(
        inboundAudit,
        INBOUND_AUDIT_PROCESS,
        EXPECTED_STOCK_INBOUND_AUDIT_EFF_PRODUCTIVITY,
        null,
        0,
        STOCK_INBOUND_AUDIT_SYS_WORKERS,
        0,
        EXPECTED_STOCK_STOCK_INBOUND_AUDIT_THROUGHPUT,
        TOTAL_STOCK_INBOUND_AUDIT_AREAS);

    assertEquals(AREA_RKL, inboundAuditAreas.get(0).getArea());
    assertEquals(0, inboundAuditAreas.get(0).getWorkers().getIdle());
    assertEquals(
        STOCK_INBOUND_AUDIT_RKL_SYS_WORKERS, inboundAuditAreas.get(0).getWorkers().getBusy());
    assertEquals(
        EXPECTED_STOCK_INBOUND_AUDIT_RKL_EFF_PRODUCTIVITY,
        inboundAuditAreas.get(0).getNetProductivity());

    assertEqualsProcess(
        stock.getProcesses().get(2),
        STOCK_AUDIT_PROCESS,
        EXPECTED_STOCK_STOCK_AUDIT_NET_PRODUCTIVITY,
        null,
        STOCK_STOCK_AUDIT_IDLE_WORKERS,
        STOCK_STOCK_AUDIT_SYS_WORKERS,
        0,
        EXPECTED_STOCK_STOCK_AUDIT_THROUGHPUT,
        0);

    assertTransferWorkflowProcesses(staffing);
  }

  @Test
  void testExecuteStaffingError() {
    // GIVEN
    final GetStaffingInput input = new GetStaffingInput(WAREHOUSE_ID);
    when(staffingGateway.getStaffing(WAREHOUSE_ID)).thenReturn(mockStaffingResponseError());

    givenSearchTrajectoriesWithProductivityForecast();
    givenSearchTrajectoriesWithoutHeadcountForecast();

    // WHEN
    final Staffing staffing = useCase.execute(input);

    // THEN
    assertNull(staffing.getTotalWorkers());
    assertEquals(TOTAL_WORKFLOWS, staffing.getWorkflows().size());

    final StaffingWorkflow outbound =
        staffing.getWorkflows().stream()
            .filter(w -> w.getWorkflow().equals(OUTBOUND_WORKFLOW))
            .findFirst()
            .orElseThrow();

    final StaffingWorkflow inbound =
        staffing.getWorkflows().stream()
            .filter(w -> w.getWorkflow().equals(INBOUND_WORKFLOW))
            .findFirst()
            .orElseThrow();

    final StaffingWorkflow withdrawals =
        staffing.getWorkflows().stream()
            .filter(w -> w.getWorkflow().equals(WITHDRAWALS_WORKFLOW))
            .findFirst()
            .orElseThrow();

    final StaffingWorkflow transfer =
        staffing.getWorkflows().stream()
            .filter(w -> w.getWorkflow().equals(TRANSFER_WORKFLOW))
            .findFirst()
            .orElseThrow();

    assertNull(outbound.getTotalWorkers());
    assertNull(outbound.getNonSystemicWorkers().getTotal());

    assertNull(inbound.getTotalWorkers());
    assertNull(inbound.getNonSystemicWorkers().getTotal());

    assertNull(withdrawals.getTotalWorkers());
    assertNull(withdrawals.getNonSystemicWorkers().getTotal());

    assertNull(transfer.getTotalWorkers());
    assertNull(transfer.getNonSystemicWorkers().getTotal());
  }

  @Test
  void testExecuteWithoutAnyWorkflow() {
    // GIVEN
    final int TOTAL_WITHOUT_ANY_WORKFLOW = 3;
    final GetStaffingInput input = new GetStaffingInput(WAREHOUSE_ID);

    when(staffingGateway.getStaffing(WAREHOUSE_ID))
        .thenReturn(mockStaffingResponseWithoutSomeWorkflow());

    givenSearchTrajectoriesWithProductivityForecast();
    givenSearchTrajectoriesWithoutHeadcountForecast();

    // WHEN
    final Staffing staffing = useCase.execute(input);

    // THEN
    final List<StaffingWorkflow> staffingWorkflows = staffing.getWorkflows();
    assertEquals(TOTAL_WITHOUT_ANY_WORKFLOW, staffingWorkflows.size());
  }

  @Test
  void testExecuteWithPlannedWorkers() {
    // GIVEN
    final GetStaffingInput input = new GetStaffingInput(WAREHOUSE_ID);

    when(staffingGateway.getStaffing(WAREHOUSE_ID))
        .thenReturn(mockStaffingResponseWithoutSomeWorkflow());

    givenSearchTrajectoriesWithProductivityForecast();
    givenSearchTrajectoriesWithHeadcountForecast();

    // WHEN
    final Staffing staffing = useCase.execute(input);

    // THEN
    final List<StaffingWorkflow> staffingWorkflows =
        staffing.getWorkflows().stream()
            .sorted(Comparator.comparing(StaffingWorkflow::getWorkflow))
            .collect(Collectors.toList());
    final StaffingWorkflow inbound = staffingWorkflows.get(0);
    final StaffingWorkflow outbound = staffingWorkflows.get(1);
    final StaffingWorkflow withdrawals = staffingWorkflows.get(2);

    // Inbound
    assertNull(inbound.getProcesses().get(0).getWorkers().getPlanned());
    assertEquals(
        FORECAST_HEADCOUNT_CHECK_IN, inbound.getProcesses().get(1).getWorkers().getPlanned());
    assertEquals(
        SIMULATION_HEADCOUNT_PUT_AWAY, inbound.getProcesses().get(2).getWorkers().getPlanned());

    assertNull(inbound.getProcesses().get(0).getWorkers().getDelta());
    assertEquals(EXPECTED_DELTA_CHECK_IN, inbound.getProcesses().get(1).getWorkers().getDelta());
    assertEquals(EXPECTED_DELTA_PUT_AWAY, inbound.getProcesses().get(2).getWorkers().getDelta());

    // Outbound
    final var outboundProcesses = outbound.getProcesses();
    assertEquals(FORECAST_HEADCOUNT_PICKING, outboundProcesses.get(0).getWorkers().getPlanned());
    assertEquals(FORECAST_HEADCOUNT_BATCH_SORTER, outboundProcesses.get(1).getWorkers().getPlanned());
    assertNull(outboundProcesses.get(2).getWorkers().getPlanned());
    assertEquals(FORECAST_HEADCOUNT_PACKING, outboundProcesses.get(3).getWorkers().getPlanned());
    assertEquals(FORECAST_HEADCOUNT_PACKING_WALL, outboundProcesses.get(4).getWorkers().getPlanned());

    assertEquals(EXPECTED_DELTA_PICKING, outboundProcesses.get(0).getWorkers().getDelta());
    assertNull(outboundProcesses.get(1).getWorkers().getDelta());
    assertNull(outboundProcesses.get(2).getWorkers().getDelta());
    assertEquals(EXPECTED_DELTA_PACKING, outboundProcesses.get(3).getWorkers().getDelta());
    assertEquals(
        EXPECTED_DELTA_PACKING_WALL, outboundProcesses.get(4).getWorkers().getDelta());

    // withdrawals
    assertNull(withdrawals.getProcesses().get(0).getWorkers().getPlanned());
    assertNull(withdrawals.getProcesses().get(1).getWorkers().getPlanned());
    assertNull(withdrawals.getProcesses().get(0).getWorkers().getDelta());
    assertNull(withdrawals.getProcesses().get(1).getWorkers().getDelta());
  }

  private void assertEqualsWorkflow(
      final StaffingWorkflow workflow,
      final String name,
      final int totalWorkers,
      final NonSystemicWorkers nsWorkers,
      final int processesSize) {

    assertEquals(name, workflow.getWorkflow());
    assertEquals(totalWorkers, workflow.getTotalWorkers());
    assertEquals(nsWorkers, workflow.getNonSystemicWorkers());
    assertEquals(processesSize, workflow.getProcesses().size());
  }

  private void assertEqualsProcess(
      final Process process,
      final String name,
      final int netProductivity,
      final Integer targetProductivity,
      final int idleWorkers,
      final int busyWorkers,
      final Integer nonSysWorkers,
      final int throughput,
      final int areasSize) {

    assertEquals(name, process.getProcess());
    assertEquals(netProductivity, process.getNetProductivity());
    assertEquals(targetProductivity, process.getTargetProductivity());
    assertEquals(busyWorkers, process.getWorkers().getBusy());
    assertEquals(idleWorkers, process.getWorkers().getIdle());
    assertEquals(nonSysWorkers, process.getWorkers().getNonSystemic());
    assertEquals(throughput, process.getThroughput());
    assertEquals(areasSize, process.getAreas().size());
  }

  private void assertNullProcess(final Process process, final String name) {
    assertEquals(name, process.getProcess());
    assertNull(process.getNetProductivity());
    assertNull(process.getTargetProductivity());
    assertNull(process.getWorkers().getBusy());
    assertNull(process.getWorkers().getIdle());
    assertNull(process.getWorkers().getNonSystemic());
    assertNull(process.getThroughput());
  }

  private StaffingResponse mockStaffingResponse() {
    return new StaffingResponse(
        List.of(
            new StaffingWorkflowResponse(
                INBOUND_WORKFLOW,
                new WorkflowTotals(INBOUND_IDLE_WORKERS, INBOUND_SYS_WORKERS, 0),
                inboundProcesses()),
            new StaffingWorkflowResponse(
                OUTBOUND_WORKFLOW,
                new WorkflowTotals(
                    OUTBOUND_IDLE_WORKERS, OUTBOUND_SYS_WORKERS, OUTBOUND_NS_WORKERS),
                outboundProcesses()),
            new StaffingWorkflowResponse(
                WITHDRAWALS_WORKFLOW,
                new WorkflowTotals(
                    WITHDRAWALS_IDLE_WORKERS, WITHDRAWALS_SYS_WORKERS, WITHDRAWALS_NS_WORKERS),
                withdrawalsProcesses()),
            new StaffingWorkflowResponse(
                STOCK_WORKFLOW,
                new WorkflowTotals(STOCK_IDLE_WORKERS, STOCK_SYS_WORKERS, STOCK_NS_WORKERS),
                stockProcesses()),
            new StaffingWorkflowResponse(
                TRANSFER_WORKFLOW,
                new WorkflowTotals(
                    TRANSFER_IDLE_WORKERS, TRANSFER_SYS_WORKERS, TRANSFER_NS_WORKERS),
                transferProcesses())));
  }

  private StaffingResponse mockStaffingResponseWithoutSomeWorkflow() {
    return new StaffingResponse(
        List.of(
            new StaffingWorkflowResponse(
                INBOUND_WORKFLOW,
                new WorkflowTotals(INBOUND_IDLE_WORKERS, INBOUND_SYS_WORKERS, 0),
                inboundProcesses()),
            new StaffingWorkflowResponse(
                OUTBOUND_WORKFLOW,
                new WorkflowTotals(
                    OUTBOUND_IDLE_WORKERS, OUTBOUND_SYS_WORKERS, OUTBOUND_NS_WORKERS),
                outboundProcesses()),
            new StaffingWorkflowResponse(
                WITHDRAWALS_WORKFLOW,
                new WorkflowTotals(
                    WITHDRAWALS_IDLE_WORKERS, WITHDRAWALS_SYS_WORKERS, WITHDRAWALS_NS_WORKERS),
                withdrawalsProcesses())));
  }

  private StaffingResponse mockStaffingResponseError() {
    return new StaffingResponse(
        List.of(
            new StaffingWorkflowResponse(
                INBOUND_WORKFLOW, new WorkflowTotals(null, null, null), inboundProcesses()),
            new StaffingWorkflowResponse(
                OUTBOUND_WORKFLOW, new WorkflowTotals(null, null, null), outboundProcesses()),
            new StaffingWorkflowResponse(
                WITHDRAWALS_WORKFLOW, new WorkflowTotals(null, null, null), withdrawalsProcesses()),
            new StaffingWorkflowResponse(
                TRANSFER_WORKFLOW, new WorkflowTotals(null, null, null), transferProcesses()),
            new StaffingWorkflowResponse(
                STOCK_WORKFLOW, new WorkflowTotals(null, null, null), stockProcesses())));
  }

  private Map<MagnitudeType, List<MagnitudePhoto>> mockProductivityForecastEntities() {
    return Map.of(
        MagnitudeType.PRODUCTIVITY,
        List.of(
            MagnitudePhoto.builder()
                .processName(ProcessName.PICKING)
                .value(FORECAST_PICKING)
                .build(),
            MagnitudePhoto.builder()
                .processName(ProcessName.WALL_IN)
                .value(FORECAST_WALL_IN)
                .build(),
            MagnitudePhoto.builder().processName(ProcessName.WAVING).value(FORECAST_WAVING).build(),
            MagnitudePhoto.builder()
                .processName(ProcessName.PACKING)
                .value(FORECAST_PACKING)
                .build()));
  }

  private void assertTransferWorkflowProcesses(final Staffing staffing) {
    final StaffingWorkflow transfer =
        staffing.getWorkflows().stream()
            .filter(w -> w.getWorkflow().equals(TRANSFER_WORKFLOW))
            .findFirst()
            .orElseThrow();

    final Process pickingTransfer = transfer.getProcesses().get(0);
    final var pickingTransferAreas = pickingTransfer.getAreas();

    assertEqualsWorkflow(
        transfer,
        TRANSFER_WORKFLOW,
        TOTAL_TRANSFER_WORKERS,
        NonSystemicWorkers.builder()
            .total(TRANSFER_NS_WORKERS)
            .cross(TRANSFER_NS_WORKERS)
            .subProcesses(0)
            .build(),
        TOTAL_TRANSFER_PROCESSES);

    assertEqualsProcess(
        pickingTransfer,
        PICKING_PROCESS,
        EXPECTED_TRANSFER_PICKING_NET_PRODUCTIVITY,
        null,
        TRANSFER_PICKING_IDLE_WORKERS,
        TRANSFER_PICKING_SYS_WORKERS,
        0,
        EXPECTED_TRANSFER_PICKING_THROUGHPUT,
        TOTAL_TRANSFER_PICKING_AREAS);

    assertEquals(AREA_RKL, pickingTransferAreas.get(0).getArea());
    assertEquals(
        TRANSFER_PICKING_RKL_IDLE_WORKERS, pickingTransferAreas.get(0).getWorkers().getIdle());
    assertEquals(
        TRANSFER_PICKING_RKL_SYS_WORKERS, pickingTransferAreas.get(0).getWorkers().getBusy());
    assertEquals(
        EXPECTED_TRANSFER_PICKING_RKL_NET_PRODUCTIVITY,
        pickingTransferAreas.get(0).getNetProductivity());
  }

  private void givenSearchTrajectoriesWithProductivityForecast() {
    final ZonedDateTime now = ZonedDateTime.now(UTC).withSecond(0).withNano(0);

    when(planningModelGateway.searchTrajectories(
        SearchTrajectoriesRequest.builder()
            .warehouseId(WAREHOUSE_ID)
            .workflow(Workflow.FBM_WMS_OUTBOUND)
            .entityTypes(List.of(MagnitudeType.PRODUCTIVITY))
            .dateFrom(now.truncatedTo(ChronoUnit.HOURS).minusHours(1))
            .dateTo(now.truncatedTo(ChronoUnit.HOURS))
            .source(Source.SIMULATION)
            .processName(
                List.of(
                    ProcessName.PICKING,
                    ProcessName.BATCH_SORTER,
                    ProcessName.WALL_IN,
                    ProcessName.PACKING,
                    ProcessName.PACKING_WALL))
            .entityFilters(
                Map.of(
                    MagnitudeType.PRODUCTIVITY,
                    Map.of(EntityFilters.ABILITY_LEVEL.toJson(), List.of(String.valueOf(1)))))
            .build()))
        .thenReturn(mockProductivityForecastEntities());
  }

  private void givenSearchTrajectoriesWithoutHeadcountForecast() {
    final ZonedDateTime now = ZonedDateTime.now(UTC).withSecond(0).withNano(0);

    when(planningModelGateway.searchTrajectories(
        SearchTrajectoriesRequest.builder()
            .warehouseId(WAREHOUSE_ID)
            .workflow(Workflow.FBM_WMS_OUTBOUND)
            .entityTypes(List.of(HEADCOUNT))
            .dateFrom(now.truncatedTo(ChronoUnit.HOURS))
            .dateTo(now.truncatedTo(ChronoUnit.HOURS))
            .processName(
                List.of(
                    ProcessName.PICKING,
                    ProcessName.BATCH_SORTER,
                    ProcessName.WALL_IN,
                    ProcessName.PACKING,
                    ProcessName.PACKING_WALL))
            .entityFilters(
                Map.of(
                    HEADCOUNT,
                    Map.of(PROCESSING_TYPE.toJson(), List.of(ACTIVE_WORKERS.getName()))))
            .build()))
        .thenThrow(MockitoException.class);

    when(planningModelGateway.searchTrajectories(
        SearchTrajectoriesRequest.builder()
            .warehouseId(WAREHOUSE_ID)
            .workflow(Workflow.FBM_WMS_INBOUND)
            .entityTypes(List.of(HEADCOUNT))
            .dateFrom(now.truncatedTo(ChronoUnit.HOURS))
            .dateTo(now.truncatedTo(ChronoUnit.HOURS))
            .processName(
                List.of(ProcessName.RECEIVING, ProcessName.CHECK_IN, ProcessName.PUT_AWAY))
            .entityFilters(
                Map.of(
                    HEADCOUNT,
                    Map.of(PROCESSING_TYPE.toJson(), List.of(ACTIVE_WORKERS.getName()))))
            .build()))
        .thenThrow(MockitoException.class);
  }

  private void givenSearchTrajectoriesWithHeadcountForecast() {
    final ZonedDateTime now = ZonedDateTime.now(UTC).withSecond(0).withNano(0);

    when(planningModelGateway.searchTrajectories(
        SearchTrajectoriesRequest.builder()
            .warehouseId(WAREHOUSE_ID)
            .workflow(Workflow.FBM_WMS_OUTBOUND)
            .entityTypes(List.of(HEADCOUNT))
            .dateFrom(now.truncatedTo(ChronoUnit.HOURS))
            .dateTo(now.truncatedTo(ChronoUnit.HOURS))
            .source(Source.SIMULATION)
            .processName(
                List.of(
                    ProcessName.PICKING,
                    ProcessName.BATCH_SORTER,
                    ProcessName.WALL_IN,
                    ProcessName.PACKING,
                    ProcessName.PACKING_WALL))
            .entityFilters(
                Map.of(
                    HEADCOUNT,
                    Map.of(PROCESSING_TYPE.toJson(), List.of(ACTIVE_WORKERS.getName()))))
            .build()))
        .thenReturn(mockHeadcountForecastEntities().get(1));

    when(planningModelGateway.searchTrajectories(
        SearchTrajectoriesRequest.builder()
            .warehouseId(WAREHOUSE_ID)
            .workflow(Workflow.FBM_WMS_INBOUND)
            .entityTypes(List.of(HEADCOUNT))
            .dateFrom(now.truncatedTo(ChronoUnit.HOURS))
            .dateTo(now.truncatedTo(ChronoUnit.HOURS))
            .source(Source.SIMULATION)
            .processName(
                List.of(ProcessName.RECEIVING, ProcessName.CHECK_IN, ProcessName.PUT_AWAY))
            .entityFilters(
                Map.of(
                    HEADCOUNT,
                    Map.of(PROCESSING_TYPE.toJson(), List.of(ACTIVE_WORKERS.getName()))))
            .build()))
        .thenReturn(mockHeadcountForecastEntities().get(0));
  }

  private List<Map<MagnitudeType, List<MagnitudePhoto>>> mockHeadcountForecastEntities() {
    return List.of(
        Map.of(
            HEADCOUNT,
            List.of(
                MagnitudePhoto.builder()
                    .processName(ProcessName.CHECK_IN)
                    .value(FORECAST_HEADCOUNT_CHECK_IN)
                    .source(Source.FORECAST)
                    .build(),
                MagnitudePhoto.builder()
                    .processName(ProcessName.PUT_AWAY)
                    .source(Source.FORECAST)
                    .value(FORECAST_HEADCOUNT_PUT_AWAY)
                    .build(),
                MagnitudePhoto.builder()
                    .processName(ProcessName.PUT_AWAY)
                    .source(Source.SIMULATION)
                    .value(SIMULATION_HEADCOUNT_PUT_AWAY)
                    .build())
        ),
        Map.of(
            HEADCOUNT,
            List.of(
                MagnitudePhoto.builder()
                    .processName(ProcessName.PICKING)
                    .value(FORECAST_HEADCOUNT_PICKING)
                    .source(Source.FORECAST)
                    .build(),
                MagnitudePhoto.builder()
                    .processName(ProcessName.PACKING_WALL)
                    .value(FORECAST_HEADCOUNT_PACKING_WALL)
                    .source(Source.FORECAST)
                    .build(),
                MagnitudePhoto.builder()
                    .processName(ProcessName.PACKING)
                    .value(FORECAST_HEADCOUNT_PACKING)
                    .source(Source.FORECAST)
                    .build(),
                MagnitudePhoto.builder()
                    .processName(ProcessName.BATCH_SORTER)
                    .value(FORECAST_HEADCOUNT_BATCH_SORTER)
                    .source(Source.FORECAST)
                    .build())
        )
    );
  }
}
