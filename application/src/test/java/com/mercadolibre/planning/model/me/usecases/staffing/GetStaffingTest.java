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
        assertEquals(122, staffing.getTotalWorkers());
        assertEquals(2, staffing.getWorkflows().size());

        final StaffingWorkflow outbound = staffing.getWorkflows().stream()
                .filter(w -> w.getWorkflow().equals("fbm-wms-outbound"))
                .findFirst().orElseThrow();

        final StaffingWorkflow inbound = staffing.getWorkflows().stream()
                .filter(w -> w.getWorkflow().equals("fbm-wms-inbound"))
                .findFirst().orElseThrow();

        assertEquals("fbm-wms-outbound", outbound.getWorkflow());
        assertEquals(102, outbound.getTotalWorkers());
        assertEquals(20, outbound.getTotalNonSystemicWorkers());
        assertEquals(5, outbound.getProcesses().size());

        final Process picking = outbound.getProcesses().get(0);
        assertEquals("picking", picking.getProcess());
        assertEquals(45, picking.getNetProductivity());
        assertEquals(30, picking.getTargetProductivity());
        assertEquals(20, picking.getWorkers().getBusy());
        assertEquals(10, picking.getWorkers().getIdle());
        assertEquals(2700, picking.getThroughput());

        final var pickingAreas = picking.getAreas();
        assertEquals(2, pickingAreas.size());
        assertEquals("MZ1", pickingAreas.get(0).getArea());
        assertEquals(10, pickingAreas.get(0).getWorkers().getBusy());
        assertEquals(0, pickingAreas.get(0).getWorkers().getIdle());
        assertEquals(60, pickingAreas.get(0).getNetProductivity());
        assertEquals("MZ2", pickingAreas.get(1).getArea());
        assertEquals(10, pickingAreas.get(1).getWorkers().getBusy());
        assertEquals(0, pickingAreas.get(0).getWorkers().getIdle());
        assertEquals(75, pickingAreas.get(1).getNetProductivity());

        final Process batchSorter = outbound.getProcesses().get(1);
        assertEquals("batch_sorter", batchSorter.getProcess());
        assertNull(batchSorter.getNetProductivity());
        assertNull(batchSorter.getTargetProductivity());
        assertNull(batchSorter.getWorkers().getBusy());
        assertNull(batchSorter.getWorkers().getIdle());
        assertNull(batchSorter.getThroughput());

        final Process wallIn = outbound.getProcesses().get(2);
        assertEquals("wall_in", wallIn.getProcess());
        assertEquals(0, wallIn.getNetProductivity());
        assertEquals(20, wallIn.getTargetProductivity());
        assertEquals(0, wallIn.getWorkers().getBusy());
        assertEquals(0, wallIn.getWorkers().getIdle());
        assertEquals(0, wallIn.getThroughput());

        final Process packing = outbound.getProcesses().get(3);
        assertEquals("packing", packing.getProcess());
        assertEquals(34, packing.getNetProductivity());
        assertEquals(35, packing.getTargetProductivity());
        assertEquals(10, packing.getWorkers().getBusy());
        assertEquals(10, packing.getWorkers().getIdle());
        assertEquals(1350, packing.getThroughput());

        assertEquals("fbm-wms-inbound", inbound.getWorkflow());
        assertEquals(20, inbound.getTotalWorkers());
        assertEquals(0, inbound.getTotalNonSystemicWorkers());
        assertEquals(3, inbound.getProcesses().size());
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
        assertEquals(2, staffing.getWorkflows().size());

        final StaffingWorkflow outbound = staffing.getWorkflows().stream()
                .filter(w -> w.getWorkflow().equals("fbm-wms-outbound"))
                .findFirst().orElseThrow();

        final StaffingWorkflow inbound = staffing.getWorkflows().stream()
                .filter(w -> w.getWorkflow().equals("fbm-wms-inbound"))
                .findFirst().orElseThrow();

        assertNull(outbound.getTotalWorkers());
        assertNull(outbound.getTotalNonSystemicWorkers());

        assertNull(inbound.getTotalWorkers());
        assertNull(inbound.getTotalNonSystemicWorkers());
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
                                outboundProcesses())
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
                                outboundProcesses())
                )
        );
    }

    private List<StaffingProcess> inboundProcesses() {
        return List.of(
                new StaffingProcess(
                        "receiving",
                        new ProcessTotals(15, 30, 25.0, null, 11.10),
                        emptyList()),
                new StaffingProcess(
                        "check_in",
                        new ProcessTotals(15, 30, 25.0, null, 11.10),
                        emptyList()),
                new StaffingProcess(
                        "put_away",
                        new ProcessTotals(15, 30, 25.0, null, 11.10),
                        List.of(
                                new Area("MZ-1", new Totals(23, 11, 32.4, 231.3)),
                                new Area("RK-L", new Totals(23, 11, 32.4, 231.3)),
                                new Area("RK-H", new Totals(23, 11, 32.4, 231.3))
                        ))
        );
    }

    private List<StaffingProcess> outboundProcesses() {
        return List.of(
                new StaffingProcess(
                        "picking",
                        new ProcessTotals(10, 20, 45.71, null, 2700.0),
                        List.of(
                                new Area("MZ1", new Totals(0, 10, 60.33, 3.3)),
                                new Area("MZ2", new Totals(0, 10, 75.42, 1.3))
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
                        new ProcessTotals(10, 10, null, 34.5, 1350.0),
                        emptyList()),
                new StaffingProcess(
                        "packing_wall",
                        new ProcessTotals(15, 30, null, 32.4, 11.10),
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
