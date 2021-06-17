package com.mercadolibre.planning.model.me.usecases.staffing;

import com.mercadolibre.planning.model.me.entities.staffing.Staffing;
import com.mercadolibre.planning.model.me.entities.staffing.StaffingWorkflow;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.staffing.StaffingGateway;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.Aggregation;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.Operation;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.Result;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.StaffingResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetStaffingTest {

    @InjectMocks
    private GetStaffing useCase;

    @Mock
    private PlanningModelGateway planningModelGateway;

    @Mock
    private StaffingGateway staffingGateway;

    @Test
    public void testExecute() {
        //GIVEN
        final GetStaffingInput input = new GetStaffingInput("ARBA01");

        when(staffingGateway.getStaffing(any())).thenReturn(mockStaffingResponse());
        when(planningModelGateway.searchEntities(any())).thenReturn(mockForecastEntities());

        //WHEN
        final Staffing staffing = useCase.execute(input);

        //THEN
        assertEquals(70, staffing.getTotalWorkers());
        assertEquals(2, staffing.getWorkflows().size());

        final StaffingWorkflow outboundO = staffing.getWorkflows().stream()
                .filter(w -> w.getWorkflow().equals("fbm-wms-outbound-order"))
                .findFirst().orElseThrow();

        final StaffingWorkflow inbound = staffing.getWorkflows().stream()
                .filter(w -> w.getWorkflow().equals("fbm-wms-inbound"))
                .findFirst().orElseThrow();

        assertEquals("fbm-wms-outbound-order", outboundO.getWorkflow());
        assertEquals(50, outboundO.getTotalWorkers());
        assertEquals(5, outboundO.getProcesses().size());

        assertEquals("picking", outboundO.getProcesses().get(0).getProcess());
        assertEquals(45, outboundO.getProcesses().get(0).getNetProductivity());
        assertEquals(30, outboundO.getProcesses().get(0).getTargetProductivity());
        assertEquals(20, outboundO.getProcesses().get(0).getWorkers().getBusy().getTotal());
        assertEquals(10, outboundO.getProcesses().get(0).getWorkers().getIdle());
        assertEquals(900, outboundO.getProcesses().get(0).getThroughput());

        assertEquals("batch_sorter", outboundO.getProcesses().get(1).getProcess());
        assertEquals(0, outboundO.getProcesses().get(1).getNetProductivity());
        assertEquals(null, outboundO.getProcesses().get(1).getTargetProductivity());
        assertEquals(0, outboundO.getProcesses().get(1).getWorkers().getBusy().getTotal());
        assertEquals(0, outboundO.getProcesses().get(1).getWorkers().getIdle());
        assertEquals(0, outboundO.getProcesses().get(1).getThroughput());

        assertEquals("wall_in", outboundO.getProcesses().get(2).getProcess());
        assertEquals(0, outboundO.getProcesses().get(2).getNetProductivity());
        assertEquals(20, outboundO.getProcesses().get(2).getTargetProductivity());
        assertEquals(0, outboundO.getProcesses().get(2).getWorkers().getBusy().getTotal());
        assertEquals(0, outboundO.getProcesses().get(2).getWorkers().getIdle());
        assertEquals(0, outboundO.getProcesses().get(2).getThroughput());

        assertEquals("packing", outboundO.getProcesses().get(3).getProcess());
        assertEquals(45, outboundO.getProcesses().get(3).getNetProductivity());
        assertEquals(35, outboundO.getProcesses().get(3).getTargetProductivity());
        assertEquals(10, outboundO.getProcesses().get(3).getWorkers().getBusy().getTotal());
        assertEquals(10, outboundO.getProcesses().get(3).getWorkers().getIdle());
        assertEquals(450, outboundO.getProcesses().get(3).getThroughput());

        assertEquals("fbm-wms-inbound", inbound.getWorkflow());
        assertEquals(20, inbound.getTotalWorkers());
        assertEquals(3, inbound.getProcesses().size());
    }

    private StaffingResponse mockStaffingResponse() {
        return new StaffingResponse(List.of(new Aggregation("staffing", List.of(
                new Result(
                        List.of("fbm-wms-outbound","order","picking","working_systemic","MZ1"),
                        List.of(new Operation("total_workers", 10),
                                new Operation("net_productivity", 45))),
                new Result(
                        List.of("fbm-wms-outbound","order","picking","working_systemic","MZ2"),
                        List.of(new Operation("total_workers", 10),
                                new Operation("net_productivity", 45))),
                new Result(
                        List.of("fbm-wms-outbound","order","picking","idle",""),
                        List.of(new Operation("total_workers", 10),
                                new Operation("net_productivity", 0))),
                new Result(
                        List.of("fbm-wms-outbound","order","packing","working_systemic",""),
                        List.of(new Operation("total_workers", 10),
                                new Operation("net_productivity", 45))),
                new Result(
                        List.of("fbm-wms-outbound","order","packing","idle",""),
                        List.of(new Operation("total_workers", 10),
                                new Operation("net_productivity", 0))),
                new Result(
                        List.of("fbm-wms-inbound","","receiving","working_systemic",""),
                        List.of(new Operation("total_workers", 10),
                                new Operation("net_productivity", 45))),
                new Result(
                        List.of("fbm-wms-inbound","","check_in","working_systemic",""),
                        List.of(new Operation("total_workers", 10),
                                new Operation("net_productivity", 45)))
                ))
        ));
    }

    private Map<EntityType, List<Entity>> mockForecastEntities() {
        return Map.of(EntityType.PRODUCTIVITY, List.of(
                Entity.builder().processName(ProcessName.PICKING).value(30).build(),
                Entity.builder().processName(ProcessName.WALL_IN).value(20).build(),
                Entity.builder().processName(ProcessName.WAVING).value(40).build(),
                Entity.builder().processName(ProcessName.PACKING).value(35).build())
        );
    }
}
