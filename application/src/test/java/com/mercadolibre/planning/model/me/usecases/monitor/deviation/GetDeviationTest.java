package com.mercadolibre.planning.model.me.usecases.monitor.deviation;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.GetDeviationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.DeviationData;
import com.mercadolibre.planning.model.me.usecases.sales.GetSales;
import com.mercadolibre.planning.model.me.usecases.sales.dtos.GetSalesInputDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.TimeZone;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.PERCENTAGE;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.TestUtils.A_DATE;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetDeviationTest {

    private static final ZonedDateTime CPT_1 = getCurrentUtcDate().plusHours(4);
    private static final ZonedDateTime CPT_2 = getCurrentUtcDate().plusHours(5);
    private static final ZonedDateTime CPT_3 = getCurrentUtcDate().plusHours(5).plusMinutes(30);
    private static final ZonedDateTime CPT_4 = getCurrentUtcDate().plusHours(6);
    private static final ZonedDateTime CPT_5 = getCurrentUtcDate().plusHours(7);

    @InjectMocks
    private GetDeviation getDeviation;

    @Mock
    private GetSales getSales;

    @Mock
    private PlanningModelGateway planningModelGateway;

    @Mock
    private LogisticCenterGateway logisticCenterGateway;

    @Test
    public void getDeviationTestOk() {
        // GIVEN
        final ZonedDateTime utcCurrentTime = getCurrentUtcDate();
        final GetDeviationInput input = GetDeviationInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .dateFrom(utcCurrentTime)
                .dateTo(utcCurrentTime.plusHours(25))
                .currentTime(A_DATE)
                .build();

        when(getSales.execute(any(GetSalesInputDto.class)))
                .thenReturn(mockSales(new int []{350, 235, 200, 120}));

        when(planningModelGateway.getPlanningDistribution(any(PlanningDistributionRequest.class)))
                .thenReturn(mockPlanningDistribution(utcCurrentTime,
                        new int[] {281, 128, 200, 207, 44, 82, 100}));

        when(planningModelGateway.getDeviation(FBM_WMS_OUTBOUND, WAREHOUSE_ID)).thenReturn(
                mockGetDeviationResponse(ZonedDateTime.of(2021,01,29,05,30,00,00, ZoneId.of("UTC")),
                        ZonedDateTime.of(2021,01,29,15,30,00,00, ZoneId.of("UTC"))));

        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
                .thenReturn(new LogisticCenterConfiguration(TimeZone.getTimeZone(
                        ZoneId.of("America/Argentina/Buenos_Aires"))));

        // WHEN
        final DeviationData deviationData = getDeviation.execute(input);

        // THEN
        assertEquals("-13.15%", deviationData.getMetrics().getDeviationPercentage()
                .getValue());
        assertNull(deviationData.getMetrics().getDeviationPercentage().getStatus());
        assertEquals("arrow_down", deviationData.getMetrics().getDeviationPercentage()
                .getIcon());
        assertEquals("905 uds.", deviationData.getMetrics().getDeviationUnits()
                .getDetail().getCurrentUnits().getValue());
        assertEquals("1042 uds.", deviationData.getMetrics().getDeviationUnits()
                .getDetail().getForecastUnits().getValue());

        assertEquals("Se ajustó el forecast 5.80% de 02:30 a 12:30", deviationData
                .getActions().getAppliedData().getTitle());
    }

    private List<PlanningDistributionResponse> mockPlanningDistribution(
            final ZonedDateTime utcCurrentTime, int[] values) {
        return List.of(
                new PlanningDistributionResponse(utcCurrentTime,
                        CPT_1,
                        MetricUnit.UNITS,
                        values[0]),
                new PlanningDistributionResponse(utcCurrentTime,
                        CPT_1,
                        MetricUnit.UNITS,
                        values[1]),
                new PlanningDistributionResponse(utcCurrentTime,
                        CPT_2,
                        MetricUnit.UNITS,
                        values[2]),
                new PlanningDistributionResponse(utcCurrentTime,
                        CPT_3,
                        MetricUnit.UNITS,
                        values[3]),
                new PlanningDistributionResponse(utcCurrentTime,
                        CPT_4,
                        MetricUnit.UNITS,
                        values[4]),
                new PlanningDistributionResponse(utcCurrentTime,
                        CPT_4,
                        MetricUnit.UNITS,
                        values[5]),
                new PlanningDistributionResponse(utcCurrentTime,
                        CPT_5,
                        MetricUnit.UNITS,
                        values[6])
        );
    }

    private List<Backlog> mockSales(int[] values) {
        return List.of(
                Backlog.builder()
                        .date(CPT_1)
                        .quantity(values[0])
                        .build(),
                Backlog.builder()
                        .date(CPT_2)
                        .quantity(values[1])
                        .build(),
                Backlog.builder()
                        .date(CPT_3)
                        .quantity(values[2])
                        .build(),
                Backlog.builder()
                        .date(CPT_4)
                        .quantity(values[3])
                        .build()
        );
    }

    @Test
    public void getDeviationTestOkStatusWarning() {
        // GIVEN
        final ZonedDateTime utcCurrentTime = getCurrentUtcDate();

        final GetDeviationInput input = GetDeviationInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .dateFrom(utcCurrentTime)
                .dateTo(utcCurrentTime.plusHours(25))
                .currentTime(A_DATE)
                .build();

        when(getSales.execute(any(GetSalesInputDto.class))
        ).thenReturn(mockSales(new int []{350, 235, 200, 120}));

        when(planningModelGateway.getPlanningDistribution(any(PlanningDistributionRequest.class)
        )).thenReturn(mockPlanningDistribution(utcCurrentTime,
                new int[] {100, 100, 120, 100, 44, 82, 100}));

        // WHEN
        final DeviationData deviationData = getDeviation.execute(input);

        // THEN
        assertEquals("40.09%", deviationData.getMetrics().getDeviationPercentage()
                .getValue());
        assertEquals("warning", deviationData.getMetrics().getDeviationPercentage().getStatus());
        assertEquals("arrow_up", deviationData.getMetrics().getDeviationPercentage()
                .getIcon());
        assertEquals("905 uds.", deviationData.getMetrics().getDeviationUnits()
                .getDetail().getCurrentUnits().getValue());
        assertEquals("646 uds.", deviationData.getMetrics().getDeviationUnits()
                .getDetail().getForecastUnits().getValue());
    }

    @Test
    public void getDeviationTestOkZeroForecast() {
        // GIVEN
        final ZonedDateTime utcCurrentTime = getCurrentUtcDate();
        final GetDeviationInput input = GetDeviationInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .dateFrom(utcCurrentTime)
                .dateTo(utcCurrentTime.plusHours(25))
                .currentTime(A_DATE)
                .build();

        when(getSales.execute(any(GetSalesInputDto.class)))
                .thenReturn(mockSales(new int []{350, 235, 200, 120}));

        when(planningModelGateway.getPlanningDistribution(any(PlanningDistributionRequest.class)))
                .thenReturn(mockPlanningDistribution(utcCurrentTime,
                        new int[] {0, 0, 0, 0, 0, 0, 0}));

        // WHEN
        final DeviationData deviationData = getDeviation.execute(input);

        // THEN
        assertEquals("0.00%", deviationData.getMetrics().getDeviationPercentage()
                .getValue());
        assertNull(deviationData.getMetrics().getDeviationPercentage().getStatus());
        assertEquals("arrow_down", deviationData.getMetrics().getDeviationPercentage()
                .getIcon());
        assertEquals("905 uds.", deviationData.getMetrics().getDeviationUnits()
                .getDetail().getCurrentUnits().getValue());
        assertEquals("0 uds.", deviationData.getMetrics().getDeviationUnits()
                .getDetail().getForecastUnits().getValue());
    }

    @Test
    public void getDeviationTestOkZeroSales() {
        // GIVEN
        final ZonedDateTime utcCurrentTime = getCurrentUtcDate();
        final GetDeviationInput input = GetDeviationInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .dateFrom(utcCurrentTime)
                .dateTo(utcCurrentTime.plusHours(25))
                .currentTime(A_DATE)
                .build();

        when(getSales.execute(any(GetSalesInputDto.class)))
                .thenReturn(mockSales(new int []{0, 0, 0, 0}));

        when(planningModelGateway.getPlanningDistribution(any(PlanningDistributionRequest.class)))
                .thenReturn(mockPlanningDistribution(utcCurrentTime,
                        new int[] {100, 100, 120, 100, 44, 82, 100}));

        // WHEN
        final DeviationData deviationData = getDeviation.execute(input);

        // THEN
        assertEquals("-100.00%", deviationData.getMetrics().getDeviationPercentage()
                .getValue());
        assertNull(deviationData.getMetrics().getDeviationPercentage().getStatus());
        assertEquals("arrow_down", deviationData.getMetrics().getDeviationPercentage()
                .getIcon());
        assertEquals("0 uds.", deviationData.getMetrics().getDeviationUnits()
                .getDetail().getCurrentUnits().getValue());
        assertEquals("646 uds.", deviationData.getMetrics().getDeviationUnits()
                .getDetail().getForecastUnits().getValue());
    }

    @Test
    public void getDeviationTestDateDeviationPassOneDay() {
        // GIVEN
        final ZonedDateTime utcCurrentTime = getCurrentUtcDate();
        final GetDeviationInput input = GetDeviationInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .dateFrom(getCurrentUtcDate())
                .dateTo(getCurrentUtcDate().plusHours(25))
                .currentTime(A_DATE)
                .build();
        when(getSales.execute(any(GetSalesInputDto.class))
        ).thenReturn(mockSales(new int []{350, 235, 200, 120}));

        when(planningModelGateway.getPlanningDistribution(any(PlanningDistributionRequest.class)
        )).thenReturn(mockPlanningDistribution(utcCurrentTime,
                new int[] {281, 128, 200, 207, 44, 82, 100}));

        when(planningModelGateway.getDeviation(FBM_WMS_OUTBOUND, WAREHOUSE_ID)).thenReturn(
                mockGetDeviationResponse(ZonedDateTime.of(2021,01,29,02,30,00,00, ZoneId.of("UTC")),
                        ZonedDateTime.of(2021,01,29,15,30,00,00, ZoneId.of("UTC"))));

        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
                .thenReturn(new LogisticCenterConfiguration(TimeZone.getTimeZone(
                        ZoneId.of("America/Argentina/Buenos_Aires"))));


        //WHEN
        final DeviationData deviationData = getDeviation.execute(input);

        //THEN
        assertEquals("Se ajustó el forecast 5.80% de 23:30 a 12:30 (+1).", deviationData
                .getActions().getAppliedData().getTitle());
    }

    private static GetDeviationResponse mockGetDeviationResponse(final ZonedDateTime dateFrom,
                                                                 final ZonedDateTime dateTo) {
        return GetDeviationResponse.builder()
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .metricUnit(PERCENTAGE)
                .value(5.8)
                .build();
    }

}