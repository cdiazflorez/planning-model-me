package com.mercadolibre.planning.model.me.usecases.staffing;

import com.mercadolibre.planning.model.me.entities.staffing.Area;
import com.mercadolibre.planning.model.me.entities.staffing.Staffing;
import com.mercadolibre.planning.model.me.entities.staffing.StaffingWorkflow;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.staffing.StaffingGateway;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.request.GetStaffingRequest;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.Aggregation;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.Operation;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.Result;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.StaffingResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDateTime;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
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
        final GetStaffingInput input = new GetStaffingInput(WAREHOUSE_ID);

        when(staffingGateway.getStaffing(mockStaffingRequest(WAREHOUSE_ID, 60)))
                .thenReturn(mockStaffingResponse(30));
        when(staffingGateway.getStaffing(mockStaffingRequest(WAREHOUSE_ID, 11)))
                .thenReturn(mockStaffingResponse(10));
        when(planningModelGateway.searchEntities(any())).thenReturn(mockForecastEntities());

        //WHEN
        final Staffing staffing = useCase.execute(input);

        //THEN
        assertEquals(70, staffing.getTotalWorkers());
        assertEquals(2, staffing.getWorkflows().size());

        final StaffingWorkflow outboundO = staffing.getWorkflows().stream()
                .filter(w -> w.getWorkflow().equals("fbm-wms-outbound"))
                .findFirst().orElseThrow();

        final StaffingWorkflow inbound = staffing.getWorkflows().stream()
                .filter(w -> w.getWorkflow().equals("fbm-wms-inbound"))
                .findFirst().orElseThrow();

        assertEquals("fbm-wms-outbound", outboundO.getWorkflow());
        assertEquals(50, outboundO.getTotalWorkers());
        assertEquals(20, outboundO.getTotalNonSystemicWorkers());
        assertEquals(5, outboundO.getProcesses().size());

        assertEquals("picking", outboundO.getProcesses().get(0).getProcess());
        assertEquals(45, outboundO.getProcesses().get(0).getNetProductivity());
        assertEquals(30, outboundO.getProcesses().get(0).getTargetProductivity());
        assertEquals(20, outboundO.getProcesses().get(0).getWorkers().getBusy());
        assertEquals(10, outboundO.getProcesses().get(0).getWorkers().getIdle());
        assertEquals(2700, outboundO.getProcesses().get(0).getThroughput());

        final List<Area> pickingAreas = outboundO.getProcesses().get(0).getAreas();

        assertEquals(2, pickingAreas.size());
        assertEquals("MZ1", pickingAreas.get(0).getArea());
        assertEquals(10, pickingAreas.get(0).getWorkers().getBusy());
        assertEquals(null, pickingAreas.get(0).getWorkers().getIdle());
        assertEquals(45, pickingAreas.get(0).getNetProductivity());
        assertEquals("MZ2", pickingAreas.get(1).getArea());
        assertEquals(10, pickingAreas.get(1).getWorkers().getBusy());
        assertEquals(null, pickingAreas.get(1).getWorkers().getIdle());
        assertEquals(45, pickingAreas.get(1).getNetProductivity());

        assertEquals("batch_sorter", outboundO.getProcesses().get(1).getProcess());
        assertEquals(0, outboundO.getProcesses().get(1).getNetProductivity());
        assertEquals(null, outboundO.getProcesses().get(1).getTargetProductivity());
        assertEquals(0, outboundO.getProcesses().get(1).getWorkers().getBusy());
        assertEquals(0, outboundO.getProcesses().get(1).getWorkers().getIdle());
        assertEquals(0, outboundO.getProcesses().get(1).getThroughput());

        assertEquals("wall_in", outboundO.getProcesses().get(2).getProcess());
        assertEquals(0, outboundO.getProcesses().get(2).getNetProductivity());
        assertEquals(20, outboundO.getProcesses().get(2).getTargetProductivity());
        assertEquals(0, outboundO.getProcesses().get(2).getWorkers().getBusy());
        assertEquals(0, outboundO.getProcesses().get(2).getWorkers().getIdle());
        assertEquals(0, outboundO.getProcesses().get(2).getThroughput());

        assertEquals("packing", outboundO.getProcesses().get(3).getProcess());
        assertEquals(45, outboundO.getProcesses().get(3).getNetProductivity());
        assertEquals(35, outboundO.getProcesses().get(3).getTargetProductivity());
        assertEquals(10, outboundO.getProcesses().get(3).getWorkers().getBusy());
        assertEquals(10, outboundO.getProcesses().get(3).getWorkers().getIdle());
        assertEquals(1350, outboundO.getProcesses().get(3).getThroughput());

        assertEquals("fbm-wms-inbound", inbound.getWorkflow());
        assertEquals(20, inbound.getTotalWorkers());
        assertEquals(0, inbound.getTotalNonSystemicWorkers());
        assertEquals(3, inbound.getProcesses().size());
    }

    private GetStaffingRequest mockStaffingRequest(final String logisticCenterId,
                                                   final int minutes) {
        final ZonedDateTime now = getCurrentUtcDateTime();

        return new GetStaffingRequest(
                now.minusMinutes(minutes),
                now,
                logisticCenterId,
                List.of(new com.mercadolibre.planning.model.me.gateways.staffing
                        .dtos.request.Aggregation(
                        "staffing",
                        List.of("workflow", "process", "worker_status", "area"),
                        List.of(new com.mercadolibre.planning.model.me.gateways.staffing.dtos
                                        .request.Operation("total_workers", "worker_id", "count"),
                                new com.mercadolibre.planning.model.me.gateways.staffing.dtos
                                        .request.Operation(
                                                "net_productivity", "net_productivity", "avg")
                        )
                ))
        );
    }

    private StaffingResponse mockStaffingResponse(final int totalWorkers) {
        return new StaffingResponse(List.of(new Aggregation("staffing", List.of(
                new Result(
                        List.of("fbm-wms-outbound","picking","working_systemic","MZ1"),
                        List.of(new Operation("total_workers", totalWorkers),
                                new Operation("net_productivity", 45))),
                new Result(
                        List.of("fbm-wms-outbound","picking","working_systemic","MZ2"),
                        List.of(new Operation("total_workers", totalWorkers),
                                new Operation("net_productivity", 45))),
                new Result(
                        List.of("fbm-wms-outbound","picking","idle",""),
                        List.of(new Operation("total_workers", totalWorkers),
                                new Operation("net_productivity", 0))),
                new Result(
                        List.of("fbm-wms-outbound","packing","working_systemic",""),
                        List.of(new Operation("total_workers", totalWorkers),
                                new Operation("net_productivity", 45))),
                new Result(
                        List.of("fbm-wms-outbound","packing","idle",""),
                        List.of(new Operation("total_workers", totalWorkers),
                                new Operation("net_productivity", 0))),
                new Result(
                        List.of("fbm-wms-inbound","receiving","working_systemic",""),
                        List.of(new Operation("total_workers", totalWorkers),
                                new Operation("net_productivity", 45))),
                new Result(
                        List.of("fbm-wms-inbound","check_in","working_systemic",""),
                        List.of(new Operation("total_workers", totalWorkers),
                                new Operation("net_productivity", 45))),
                new Result(
                        List.of("fbm-wms-outbound","out_setter","working_non_systemic",""),
                        List.of(new Operation("total_workers", totalWorkers),
                                new Operation("net_productivity", 45))),
                new Result(
                        List.of("fbm-wms-outbound","sorting_carrousel","working_non_systemic",""),
                        List.of(new Operation("total_workers", totalWorkers),
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
