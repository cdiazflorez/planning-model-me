package com.mercadolibre.planning.model.me.usecases.staffing;

import com.mercadolibre.planning.model.me.entities.staffing.PlannedHeadcount;
import com.mercadolibre.planning.model.me.entities.staffing.PlannedHeadcountByHour;
import com.mercadolibre.planning.model.me.entities.staffing.PlannedHeadcountByProcess;
import com.mercadolibre.planning.model.me.entities.staffing.PlannedHeadcountByWorkflow;
import com.mercadolibre.planning.model.me.exception.NoPlannedDataException;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagVarPhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.staffing.dtos.GetPlannedHeadcountInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.me.utils.DateUtils.HOUR_MINUTES_FORMATTER;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WORKFLOW;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetPlannedHeadcountTest {

    @InjectMocks
    private GetPlannedHeadcount useCase;

    @Mock
    private LogisticCenterGateway logisticCenterGateway;

    @Mock
    private PlanningModelGateway planningModelGateway;

    @Test
    public void testExecuteOk() {
        // GIVEN
        final GetPlannedHeadcountInput input = new GetPlannedHeadcountInput(WAREHOUSE_ID);

        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
                .thenReturn(new LogisticCenterConfiguration(TimeZone.getDefault()));
        when(planningModelGateway.searchTrajectories(any())).thenReturn(mockEntities());

        // WHEN
        final PlannedHeadcount plannedHeadcount = useCase.execute(input);

        // THEN
        assertTrue(plannedHeadcount.getHeadcountByHours().size() > 0);
        assertEquals(getExpectedResult(), plannedHeadcount);
    }

    @Test
    public void testExecuteError() {
        // GIVEN
        final GetPlannedHeadcountInput input = new GetPlannedHeadcountInput(WAREHOUSE_ID);

        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
                .thenReturn(new LogisticCenterConfiguration(TimeZone.getDefault()));

        when(planningModelGateway.searchTrajectories(any()))
                .thenThrow(NoPlannedDataException.class);

        // WHEN - THEN
        assertThrows(NoPlannedDataException.class, () -> useCase.execute(input));
    }

    private PlannedHeadcount getExpectedResult() {
        final ZonedDateTime now = ZonedDateTime.now().truncatedTo(DAYS);

        return new PlannedHeadcount(List.of(
                new PlannedHeadcountByHour(
                        now.format(HOUR_MINUTES_FORMATTER),
                        List.of(
                                new PlannedHeadcountByWorkflow(WORKFLOW.getName(), 17, List.of(
                                        new PlannedHeadcountByProcess(PACKING.getName(), 5, 58),
                                        new PlannedHeadcountByProcess(PICKING.getName(), 10, 100),
                                        new PlannedHeadcountByProcess(WALL_IN.getName(), 2, 22)
                                        ))
                        )
                ),
                new PlannedHeadcountByHour(
                        now.plusHours(1).format(HOUR_MINUTES_FORMATTER),
                        List.of(
                                new PlannedHeadcountByWorkflow(WORKFLOW.getName(), 26, List.of(
                                        new PlannedHeadcountByProcess(PACKING.getName(), 8, 82),
                                        new PlannedHeadcountByProcess(PICKING.getName(), 15, 120),
                                        new PlannedHeadcountByProcess(WALL_IN.getName(), 3, 33)
                                ))
                        )
                )
        ));
    }

    private Map<MagnitudeType, List<MagVarPhoto>> mockEntities() {
        final ZonedDateTime now = ZonedDateTime.now().truncatedTo(DAYS);

        return Map.of(
                MagnitudeType.HEADCOUNT, List.of(
                        MagVarPhoto.builder()
                                .date(now)
                                .workflow(Workflow.FBM_WMS_OUTBOUND)
                                .processName(PICKING)
                                .metricUnit(MetricUnit.WORKERS)
                                .value(10)
                                .build(),
                        MagVarPhoto.builder()
                                .date(now.plusHours(1))
                                .workflow(Workflow.FBM_WMS_OUTBOUND)
                                .processName(PICKING)
                                .metricUnit(MetricUnit.WORKERS)
                                .value(15)
                                .build(),
                        MagVarPhoto.builder()
                                .date(now)
                                .workflow(Workflow.FBM_WMS_OUTBOUND)
                                .processName(PACKING)
                                .metricUnit(MetricUnit.WORKERS)
                                .value(5)
                                .build(),
                        MagVarPhoto.builder()
                                .date(now.plusHours(1))
                                .workflow(Workflow.FBM_WMS_OUTBOUND)
                                .processName(PACKING)
                                .metricUnit(MetricUnit.WORKERS)
                                .value(8)
                                .build(),
                        MagVarPhoto.builder()
                                .date(now)
                                .workflow(Workflow.FBM_WMS_OUTBOUND)
                                .processName(WALL_IN)
                                .metricUnit(MetricUnit.WORKERS)
                                .value(2)
                                .build(),
                        MagVarPhoto.builder()
                                .date(now.plusHours(1))
                                .workflow(Workflow.FBM_WMS_OUTBOUND)
                                .processName(WALL_IN)
                                .metricUnit(MetricUnit.WORKERS)
                                .value(3)
                                .build()
                ),
                MagnitudeType.THROUGHPUT, List.of(
                        MagVarPhoto.builder()
                                .date(now)
                                .workflow(Workflow.FBM_WMS_OUTBOUND)
                                .processName(PICKING)
                                .metricUnit(MetricUnit.UNITS_PER_HOUR)
                                .value(100)
                                .build(),
                        MagVarPhoto.builder()
                                .date(now.plusHours(1))
                                .workflow(Workflow.FBM_WMS_OUTBOUND)
                                .processName(PICKING)
                                .metricUnit(MetricUnit.UNITS_PER_HOUR)
                                .value(120)
                                .build(),
                        MagVarPhoto.builder()
                                .date(now)
                                .workflow(Workflow.FBM_WMS_OUTBOUND)
                                .processName(PACKING)
                                .metricUnit(MetricUnit.UNITS_PER_HOUR)
                                .value(58)
                                .build(),
                        MagVarPhoto.builder()
                                .date(now.plusHours(1))
                                .workflow(Workflow.FBM_WMS_OUTBOUND)
                                .processName(PACKING)
                                .metricUnit(MetricUnit.UNITS_PER_HOUR)
                                .value(82)
                                .build(),
                        MagVarPhoto.builder()
                                .date(now)
                                .workflow(Workflow.FBM_WMS_OUTBOUND)
                                .processName(WALL_IN)
                                .metricUnit(MetricUnit.UNITS_PER_HOUR)
                                .value(22)
                                .build(),
                        MagVarPhoto.builder()
                                .date(now.plusHours(1))
                                .workflow(Workflow.FBM_WMS_OUTBOUND)
                                .processName(WALL_IN)
                                .metricUnit(MetricUnit.UNITS_PER_HOUR)
                                .value(33)
                                .build()
                )
        );
    }
}
