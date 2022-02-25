package com.mercadolibre.planning.model.me.usecases.staffing;

import com.mercadolibre.planning.model.me.entities.staffing.Process;
import com.mercadolibre.planning.model.me.entities.staffing.Staffing;
import com.mercadolibre.planning.model.me.entities.staffing.StaffingWorkflow;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.staffing.StaffingGateway;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.Area;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.ProcessTotals;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.StaffingProcess;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.StaffingResponse;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.StaffingWorkflowResponse;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.Totals;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.WorkflowTotals;
import com.mercadolibre.planning.model.me.usecases.staffing.dtos.GetStaffingInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetStaffingTest {

    private static String OUTBOUND_WORKFLOW = "fbm-wms-outbound";
    private static String INBOUND_WORKFLOW = "fbm-wms-inbound";
    private static String WITHDRAWALS_WORKFLOW = "fbm-wms-withdrawals";
    private static String RECEIVING_PROCESS = "receiving";
    private static String CHECK_IN_PROCESS = "check_in";
    private static String PUT_AWAY_PROCESS = "put_away";
    private static String PICKING_PROCESS = "picking";
    private static String BATCH_SORTER_PROCESS = "batch_sorter";
    private static String WALL_IN_PROCESS = "wall_in";
    private static String PACKING_PROCESS = "packing";
    private static String PACKING_WALL_PROCESS = "packing_wall";
    private static String AREA_MZ1 = "MZ-1";
    private static String AREA_RKL = "RK-L";



    @InjectMocks
    private GetStaffing useCase;

    @Mock
    private PlanningModelGateway planningModelGateway;

    @Mock
    private StaffingGateway staffingGateway;

    @Test
    void testExecute() {
        //GIVEN
        final GetStaffingInput input = new GetStaffingInput(WAREHOUSE_ID);

        when(staffingGateway.getStaffing(WAREHOUSE_ID))
                .thenReturn(mockStaffingResponse());

        when(planningModelGateway.searchTrajectories(any()))
                .thenReturn(mockForecastEntities());

        //WHEN
        final Staffing staffing = useCase.execute(input);

        //THEN
        final StaffingWorkflow outbound = staffing.getWorkflows().stream()
                .filter(w -> w.getWorkflow().equals(OUTBOUND_WORKFLOW))
                .findFirst().orElseThrow();

        final StaffingWorkflow inbound = staffing.getWorkflows().stream()
                .filter(w -> w.getWorkflow().equals(INBOUND_WORKFLOW))
                .findFirst().orElseThrow();

        final StaffingWorkflow withdrawals = staffing.getWorkflows().stream()
                .filter(w -> w.getWorkflow().equals(WITHDRAWALS_WORKFLOW))
                .findFirst().orElseThrow();

        final Process putAway = inbound.getProcesses().get(2);
        final Process picking = outbound.getProcesses().get(0);
        final Process pickingWithdrawals = withdrawals.getProcesses().get(0);
        final var putAwayAreas = putAway.getAreas();
        final var pickingAreas = picking.getAreas();
        final var pickingWithdrawalsAreas = pickingWithdrawals.getAreas();

        assertEquals(176, staffing.getTotalWorkers());
        assertEquals(3, staffing.getWorkflows().size());

        assertEqualsWorkflow(inbound, INBOUND_WORKFLOW, 20,0, 3);
        assertEqualsProcess(inbound.getProcesses().get(0), RECEIVING_PROCESS, 25, null, 0, 3, 1, 0);
        assertEqualsProcess(inbound.getProcesses().get(1), CHECK_IN_PROCESS, 35, null, 1, 6, 11, 0);
        assertEqualsProcess(putAway, PUT_AWAY_PROCESS, 15, null, 1, 9, 21, 2);
        assertEquals(AREA_MZ1, putAwayAreas.get(0).getArea());
        assertEquals(0, putAwayAreas.get(0).getWorkers().getIdle());
        assertEquals(5, putAwayAreas.get(0).getWorkers().getBusy());
        assertEquals(32, putAwayAreas.get(0).getNetProductivity());
        assertEquals(AREA_RKL, putAwayAreas.get(1).getArea());
        assertEquals(1, putAwayAreas.get(1).getWorkers().getIdle());
        assertEquals(4, putAwayAreas.get(1).getWorkers().getBusy());
        assertEquals(32, putAwayAreas.get(1).getNetProductivity());


        assertEqualsWorkflow(outbound, OUTBOUND_WORKFLOW, 102,20, 5);
        assertEqualsProcess(picking, PICKING_PROCESS, 45, 30, 6, 20, 2700, 2);
        assertEquals(AREA_MZ1, pickingAreas.get(0).getArea());
        assertEquals(4, pickingAreas.get(0).getWorkers().getIdle());
        assertEquals(10, pickingAreas.get(0).getWorkers().getBusy());
        assertEquals(60, pickingAreas.get(0).getNetProductivity());
        assertEquals(AREA_RKL, pickingAreas.get(1).getArea());
        assertEquals(2, pickingAreas.get(1).getWorkers().getIdle());
        assertEquals(10, pickingAreas.get(1).getWorkers().getBusy());
        assertEquals(75, pickingAreas.get(1).getNetProductivity());
        assertNullProcess(outbound.getProcesses().get(1), BATCH_SORTER_PROCESS);
        assertEqualsProcess(outbound.getProcesses().get(2), WALL_IN_PROCESS, 0, 20, 0, 0, 0, 0);
        assertEqualsProcess(outbound.getProcesses().get(3), PACKING_PROCESS, 34, 35, 1, 15, 1350, 0);
        assertEqualsProcess(outbound.getProcesses().get(4), PACKING_WALL_PROCESS, 32, null, 5, 35, 11, 0);

        assertEqualsWorkflow(withdrawals, WITHDRAWALS_WORKFLOW, 54,24, 2);
        assertEqualsProcess(pickingWithdrawals, PICKING_PROCESS, 45, null, 10, 14, 2700, 2);
        assertEquals(2, pickingWithdrawalsAreas.size());
        assertEquals("MZ-1", pickingWithdrawalsAreas.get(0).getArea());
        assertEquals(6, pickingWithdrawalsAreas.get(0).getWorkers().getIdle());
        assertEquals(8, pickingWithdrawalsAreas.get(0).getWorkers().getBusy());
        assertEquals(60, pickingWithdrawalsAreas.get(0).getNetProductivity());
        assertEquals("RK-L", pickingWithdrawalsAreas.get(1).getArea());
        assertEquals(4, pickingWithdrawalsAreas.get(1).getWorkers().getIdle());
        assertEquals(6, pickingWithdrawalsAreas.get(1).getWorkers().getBusy());
        assertEquals(75, pickingWithdrawalsAreas.get(1).getNetProductivity());
        assertEqualsProcess(withdrawals.getProcesses().get(1), PACKING_PROCESS, 34, null, 2, 4, 1350, 0);
    }

    @Test
    void testExecuteStaffingError() {
        //GIVEN
        final GetStaffingInput input = new GetStaffingInput(WAREHOUSE_ID);

        when(staffingGateway.getStaffing(WAREHOUSE_ID))
                .thenReturn(mockStaffingResponseError());

        when(planningModelGateway.searchTrajectories(any()))
                .thenReturn(mockForecastEntities());

        //WHEN
        final Staffing staffing = useCase.execute(input);

        //THEN
        assertNull(staffing.getTotalWorkers());
        assertEquals(3, staffing.getWorkflows().size());

        final StaffingWorkflow outbound = staffing.getWorkflows().stream()
                .filter(w -> w.getWorkflow().equals("fbm-wms-outbound"))
                .findFirst().orElseThrow();

        final StaffingWorkflow inbound = staffing.getWorkflows().stream()
                .filter(w -> w.getWorkflow().equals("fbm-wms-inbound"))
                .findFirst().orElseThrow();

        final StaffingWorkflow withdrawals = staffing.getWorkflows().stream()
                .filter(w -> w.getWorkflow().equals("fbm-wms-withdrawals"))
                .findFirst().orElseThrow();

        assertNull(outbound.getTotalWorkers());
        assertNull(outbound.getTotalNonSystemicWorkers());

        assertNull(inbound.getTotalWorkers());
        assertNull(inbound.getTotalNonSystemicWorkers());

        assertNull(withdrawals.getTotalWorkers());
        assertNull(withdrawals.getTotalNonSystemicWorkers());
    }

    private void assertEqualsWorkflow(final StaffingWorkflow workflow,
                                              final String name,
                                              final int totalWorkers,
                                              final int nsWorkers,
                                              final int processesSize) {

        assertEquals(name, workflow.getWorkflow());
        assertEquals(totalWorkers, workflow.getTotalWorkers());
        assertEquals(nsWorkers, workflow.getTotalNonSystemicWorkers());
        assertEquals(processesSize, workflow.getProcesses().size());
    }

    private void assertEqualsProcess(final Process process, final String name, final int netProductivity,
                                     final Integer targetProductivity, final int idleWorkers, final int busyWorkers,
                                     final int throughput, final int areasSize) {

        assertEquals(name, process.getProcess());
        assertEquals(netProductivity, process.getNetProductivity());
        assertEquals(targetProductivity, process.getTargetProductivity());
        assertEquals(busyWorkers, process.getWorkers().getBusy());
        assertEquals(idleWorkers, process.getWorkers().getIdle());
        assertEquals(throughput, process.getThroughput());
        assertEquals(areasSize, process.getAreas().size());
    }

    private void assertNullProcess(final Process process, final String name) {
        assertEquals(name, process.getProcess());
        assertNull(process.getNetProductivity());
        assertNull(process.getTargetProductivity());
        assertNull(process.getWorkers().getBusy());
        assertNull(process.getWorkers().getIdle());
        assertNull(process.getThroughput());
    }

    private StaffingResponse mockStaffingResponse() {
        return new StaffingResponse(
                List.of(
                        new StaffingWorkflowResponse(
                                "fbm-wms-inbound",
                                new WorkflowTotals(2, 18, 0),
                                inboundProcesses()),
                        new StaffingWorkflowResponse(
                                "fbm-wms-outbound",
                                new WorkflowTotals(12, 70, 20),
                                outboundProcesses()),
                        new StaffingWorkflowResponse(
                                "fbm-wms-withdrawals",
                                new WorkflowTotals(12, 18, 24),
                                withdrawalsProcesses())
                )
        );
    }

    private StaffingResponse mockStaffingResponseError() {
        return new StaffingResponse(
                List.of(
                        new StaffingWorkflowResponse(
                                "fbm-wms-inbound",
                                new WorkflowTotals(null, null, null),
                                inboundProcesses()),
                        new StaffingWorkflowResponse(
                                "fbm-wms-outbound",
                                new WorkflowTotals(null, null, null),
                                outboundProcesses()),
                        new StaffingWorkflowResponse(
                                "fbm-wms-withdrawals",
                                new WorkflowTotals(null, null, null),
                                outboundProcesses())
                )
        );
    }

    private List<StaffingProcess> inboundProcesses() {
        return List.of(
                new StaffingProcess(
                        "receiving",
                        new ProcessTotals(0, 3, 25.0, null, 1.10),
                        emptyList()),
                new StaffingProcess(
                        "check_in",
                        new ProcessTotals(1, 6, 35.0, null, 11.10),
                        emptyList()),
                new StaffingProcess(
                        "put_away",
                        new ProcessTotals(1, 9, 15.0, null, 21.10),
                        List.of(
                                new Area("MZ-1", new Totals(0, 5, 32.4, 231.3)),
                                new Area("RK-L", new Totals(1, 4, 32.4, 231.3))
                        ))
        );
    }

    private List<StaffingProcess> outboundProcesses() {
        return List.of(
                new StaffingProcess(
                        "picking",
                        new ProcessTotals(6, 20, 45.71, null, 2700.0),
                        List.of(
                                new Area("MZ-1", new Totals(4, 10, 60.33, 3.3)),
                                new Area("RK-L", new Totals(2, 10, 75.42, 1.3))
                        )),
                new StaffingProcess(
                        "batch_sorter",
                        new ProcessTotals(null, null, null, null, null),
                        emptyList()),
                new StaffingProcess(
                        "wall_in",
                        new ProcessTotals(0, 0, 0.0, null, 0.0),
                        emptyList()),
                new StaffingProcess(
                        "packing",
                        new ProcessTotals(1, 15, null, 34.5, 1350.0),
                        emptyList()),
                new StaffingProcess(
                        "packing_wall",
                        new ProcessTotals(5, 35, null, 32.4, 11.10),
                        emptyList())
        );
    }

    private List<StaffingProcess> withdrawalsProcesses() {
        return List.of(
                new StaffingProcess(
                        "picking",
                        new ProcessTotals(10, 14, 45.71, null, 2700.0),
                        List.of(
                                new Area("MZ-1", new Totals(6, 8, 60.33, 3.3)),
                                new Area("RK-L", new Totals(4, 6, 75.42, 1.3))
                        )),
                new StaffingProcess(
                        "packing",
                        new ProcessTotals(2, 4, null, 34.5, 1350.0),
                        emptyList())
        );
    }

    private Map<MagnitudeType, List<MagnitudePhoto>> mockForecastEntities() {
        return Map.of(MagnitudeType.PRODUCTIVITY, List.of(
                MagnitudePhoto.builder().processName(ProcessName.PICKING).value(30).build(),
                MagnitudePhoto.builder().processName(ProcessName.WALL_IN).value(20).build(),
                MagnitudePhoto.builder().processName(ProcessName.WAVING).value(40).build(),
                MagnitudePhoto.builder().processName(ProcessName.PACKING).value(35).build()
        ));
    }
}
