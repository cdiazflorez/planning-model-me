package com.mercadolibre.planning.model.me.usecases.projection;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.ComplexTable;
import com.mercadolibre.planning.model.me.entities.projection.Data;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.ProjectionResult;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.entities.projection.chart.Chart;
import com.mercadolibre.planning.model.me.entities.projection.chart.ChartData;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ConfigurationRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ConfigurationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Productivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProductivityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklog;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogInputDto;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.sales.GetSales;
import com.mercadolibre.planning.model.me.usecases.sales.dtos.GetSalesInputDto;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.GetWaveSuggestion;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.dto.GetWaveSuggestionInputDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.IntStream;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MONO_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_BATCH_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.TestUtils.PROCESSING_TIME;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.TimeZone.getDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetForecastProjectionTest {

    private static final DateTimeFormatter HOUR_FORMAT = ofPattern("HH:00");
    private static final DateTimeFormatter HOUR_MINUTES_FORMAT = ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    private static final TimeZone TIME_ZONE = getDefault();
    private static final ZonedDateTime CPT_1 = getCurrentUtcDate().plusHours(4);
    private static final ZonedDateTime CPT_2 = getCurrentUtcDate().plusHours(5);
    private static final ZonedDateTime CPT_3 = getCurrentUtcDate().plusHours(5).plusMinutes(30);
    private static final ZonedDateTime CPT_4 = getCurrentUtcDate().plusHours(6);
    private static final ZonedDateTime CPT_5 = getCurrentUtcDate().plusHours(7);

    @InjectMocks
    private GetForecastProjection getProjection;

    @Mock
    private PlanningModelGateway planningModelGateway;

    @Mock
    private LogisticCenterGateway logisticCenterGateway;

    @Mock
    private GetBacklog getBacklog;

    @Mock
    private GetSales getSales;

    @Mock
    private GetWaveSuggestion getWaveSuggestion;

    @Test
    void testExecute() {
        // Given
        final ZonedDateTime utcCurrentTime = getCurrentUtcDate();

        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
                .thenReturn(new LogisticCenterConfiguration(TIME_ZONE));

        when(planningModelGateway.getEntities(
                createRequest(HEADCOUNT, utcCurrentTime, List.of(ProcessingType.ACTIVE_WORKERS))))
                .thenReturn(mockHeadcountEntities(utcCurrentTime));

        when(planningModelGateway.getProductivity(createProductivityRequest(utcCurrentTime)))
                .thenReturn(mockProductivityEntities(utcCurrentTime));

        when(planningModelGateway.getEntities(createRequest(THROUGHPUT, utcCurrentTime)))
                .thenReturn(mockThroughputEntities());

        when(planningModelGateway.getConfiguration(createConfigurationRequest()))
                .thenReturn(mockProcessingTimeConfiguration());

        final List<Backlog> mockedBacklog = mockBacklog();
        when(getBacklog.execute(new GetBacklogInputDto(FBM_WMS_OUTBOUND, WAREHOUSE_ID)))
                .thenReturn(mockedBacklog);

        when(getSales.execute(new GetSalesInputDto(
                FBM_WMS_OUTBOUND, WAREHOUSE_ID, utcCurrentTime.minusHours(28)))
        ).thenReturn(mockSales());

        when(planningModelGateway.runProjection(
                createProjectionRequest(mockedBacklog, utcCurrentTime)))
                .thenReturn(mockProjections(utcCurrentTime));

        when(planningModelGateway.getPlanningDistribution(new PlanningDistributionRequest(
                WAREHOUSE_ID,
                FBM_WMS_OUTBOUND,
                utcCurrentTime,
                utcCurrentTime,
                utcCurrentTime.plusDays(1))
        )).thenReturn(mockPlanningDistribution(utcCurrentTime));

        final ZonedDateTime currentUtcDateTime = getCurrentUtcDate();
        final ZonedDateTime utcDateTimeFrom = currentUtcDateTime.plusHours(1);
        final ZonedDateTime utcDateTimeTo = currentUtcDateTime.plusHours(2);

        when(getWaveSuggestion.execute((GetWaveSuggestionInputDto.builder()
                        .warehouseId(WAREHOUSE_ID)
                        .workflow(FBM_WMS_OUTBOUND)
                        .zoneId(TIME_ZONE.toZoneId())
                        .build()
                )
        )).thenReturn(mockSuggestedWaves(utcDateTimeFrom, utcDateTimeTo));

        // When
        final Projection projection = getProjection.execute(GetProjectionInputDto.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .build()
        );

        // Then
        assertEquals("Proyecciones", projection.getTitle());

        assertSimpleTable(projection.getSimpleTable1(), utcDateTimeFrom, utcDateTimeTo);
        assertComplexTable(projection.getComplexTable1());
        assertChart(projection.getChart());
        assertProjectionDetailsTable(projection.getSimpleTable2());
    }

    private void assertSimpleTable(final SimpleTable simpleTable,
                                   final ZonedDateTime utcDateTimeFrom,
                                   final ZonedDateTime utcDateTimeTo) {
        List<Map<String, Object>> data = simpleTable.getData();
        assertEquals(3, data.size());
        assertEquals("0 uds.", data.get(0).get("column_2"));
        final Map<String, Object> column1Mono = (Map<String, Object>) data.get(0).get("column_1");
        assertEquals(MONO_ORDER_DISTRIBUTION.getTitle(), column1Mono.get("subtitle"));

        assertEquals("100 uds.", data.get(1).get("column_2"));
        final Map<String, Object> column1Multi = (Map<String, Object>) data.get(1).get("column_1");
        assertEquals(MULTI_BATCH_DISTRIBUTION.getTitle(), column1Multi.get("subtitle"));

        assertEquals("100 uds.", data.get(1).get("column_2"));
        final Map<String, Object> column1MultiBatch = (Map<String, Object>) data.get(2).get(
                "column_1");
        assertEquals(MULTI_ORDER_DISTRIBUTION.getTitle(), column1MultiBatch.get("subtitle"));

        final String title = simpleTable.getColumns().get(0).getTitle();
        final String nextHour = utcDateTimeFrom.withZoneSameInstant(TIME_ZONE.toZoneId())
                .format(ofPattern("HH:mm")) + "-"
                + utcDateTimeTo.withZoneSameInstant(TIME_ZONE.toZoneId())
                .format(ofPattern("HH:mm"));
        final String expextedTitle = "Sig. hora " + nextHour;
        assertEquals(title, expextedTitle);
    }

    private void assertProjectionDetailsTable(final SimpleTable projectionDetailsTable) {
        final ZoneId zoneId = TIME_ZONE.toZoneId();
        final ZonedDateTime currentTime = convertToTimeZone(zoneId, getCurrentUtcDate());

        assertEquals("Resumen de Proyección", projectionDetailsTable.getTitle());
        assertEquals(4, projectionDetailsTable.getColumns().size());
        assertEquals(5, projectionDetailsTable.getData().size());

        final Map<String, Object> cpt1 = projectionDetailsTable.getData().get(4);
        final Map<String, Object> cpt2 = projectionDetailsTable.getData().get(3);
        final Map<String, Object> cpt3 = projectionDetailsTable.getData().get(2);
        final Map<String, Object> cpt4 = projectionDetailsTable.getData().get(1);
        final Map<String, Object> cpt5 = projectionDetailsTable.getData().get(0);

        assertEquals("warning", cpt1.get("style"));
        assertEquals(convertToTimeZone(zoneId, CPT_1).format(HOUR_MINUTES_FORMAT),
                cpt1.get("column_1"));
        assertEquals("150", cpt1.get("column_2"));
        assertEquals("-14.4%", cpt1.get("column_3"));
        assertEquals(currentTime.plusHours(3).plusMinutes(30).format(HOUR_MINUTES_FORMAT),
                cpt1.get("column_4"));

        assertEquals("none", cpt2.get("style"));
        assertEquals(convertToTimeZone(zoneId, CPT_2).format(HOUR_MINUTES_FORMAT),
                cpt2.get("column_1"));
        assertEquals("235", cpt2.get("column_2"));
        assertEquals("17.5%", cpt2.get("column_3"));
        assertEquals(currentTime.plusHours(3).format(HOUR_MINUTES_FORMAT), cpt2.get("column_4"));

        assertEquals("none", cpt3.get("style"));
        assertEquals(convertToTimeZone(zoneId, CPT_3).format(HOUR_MINUTES_FORMAT),
                cpt3.get("column_1"));
        assertEquals("300", cpt3.get("column_2"));
        assertEquals("-3.4%", cpt3.get("column_3"));
        assertEquals(currentTime.plusHours(3).plusMinutes(25).format(HOUR_MINUTES_FORMAT),
                cpt3.get("column_4"));

        assertEquals("danger", cpt4.get("style"));
        assertEquals(convertToTimeZone(zoneId, CPT_4).format(HOUR_MINUTES_FORMAT),
                cpt4.get("column_1"));
        assertEquals("120", cpt4.get("column_2"));
        assertEquals("-4.8%", cpt4.get("column_3"));
        assertEquals(currentTime.plusHours(8).plusMinutes(10).format(HOUR_MINUTES_FORMAT),
                cpt4.get("column_4"));

        assertEquals("danger", cpt5.get("style"));
        assertEquals(convertToTimeZone(zoneId, CPT_5).format(HOUR_MINUTES_FORMAT),
                cpt5.get("column_1"));
        assertEquals("0", cpt5.get("column_2"));
        assertEquals("0%", cpt5.get("column_3"));
        assertEquals("Excede las 24hs", cpt5.get("column_4"));
    }

    private void assertChart(final Chart chart) {
        final ZoneId zoneId = TIME_ZONE.toZoneId();
        final List<ChartData> chartData = chart.getData();
        final ChartData chartData1 = chartData.get(0);
        final ChartData chartData2 = chartData.get(1);
        final ChartData chartData3 = chartData.get(2);
        final ChartData chartData4 = chartData.get(3);
        final ChartData chartData5 = chartData.get(4);

        assertEquals(60, chart.getProcessingTime().getValue());
        assertEquals(5, chartData.size());

        assertEquals(convertToTimeZone(zoneId, CPT_1).format(HOUR_MINUTES_FORMAT),
                chartData1.getTitle());
        assertEquals(convertToTimeZone(zoneId, CPT_1).format(DATE_FORMATTER), chartData1.getCpt());
        assertEquals(convertToTimeZone(zoneId, getCurrentUtcDate()).plusHours(3).plusMinutes(30)
                        .format(DATE_FORMATTER), chartData1.getProjectedEndTime());

        assertEquals(convertToTimeZone(zoneId, CPT_2).format(HOUR_MINUTES_FORMAT),
                chartData2.getTitle());
        assertEquals(convertToTimeZone(zoneId, CPT_2).format(DATE_FORMATTER), chartData2.getCpt());
        assertEquals(convertToTimeZone(zoneId, getCurrentUtcDate()).plusHours(3)
                        .format(DATE_FORMATTER), chartData2.getProjectedEndTime());

        assertEquals(convertToTimeZone(zoneId, CPT_3).format(HOUR_MINUTES_FORMAT),
                chartData3.getTitle());
        assertEquals(convertToTimeZone(zoneId, CPT_3).format(DATE_FORMATTER), chartData3.getCpt());
        assertEquals(convertToTimeZone(zoneId, getCurrentUtcDate()).plusHours(3).plusMinutes(25)
                        .format(DATE_FORMATTER), chartData3.getProjectedEndTime());

        assertEquals(convertToTimeZone(zoneId, CPT_4).toLocalTime().format(HOUR_MINUTES_FORMAT),
                chartData4.getTitle());
        assertEquals(convertToTimeZone(zoneId, CPT_4).format(DATE_FORMATTER), chartData4.getCpt());
        assertEquals(convertToTimeZone(zoneId, getCurrentUtcDate()).plusHours(8).plusMinutes(10)
                        .format(DATE_FORMATTER), chartData4.getProjectedEndTime());

        assertEquals(convertToTimeZone(zoneId, CPT_5).toLocalTime().format(HOUR_MINUTES_FORMAT),
                chartData5.getTitle());
        assertEquals(convertToTimeZone(zoneId, CPT_5).format(DATE_FORMATTER), chartData5.getCpt());
        assertEquals(convertToTimeZone(zoneId, getCurrentUtcDate()).plusDays(1)
                        .format(DATE_FORMATTER), chartData5.getProjectedEndTime());
    }

    private void assertComplexTable(final ComplexTable complexTable) {
        final ZonedDateTime currentTime = getCurrentUtcDate()
                .withZoneSameInstant(TIME_ZONE.toZoneId());
        final List<ColumnHeader> columns = complexTable.getColumns();
        final List<Data> data = complexTable.getData();

        IntStream.range(0, 24).forEach(index -> {
            assertEquals("column_" + (index + 2), columns.get(index + 1).getId());
            assertEquals(currentTime.plusHours(index).format(HOUR_FORMAT),
                    columns.get(index + 1).getTitle());
        });
        assertEquals(3, data.size());

        final Data headcount = data.get(0);
        final Data productivity = data.get(1);
        final Data throughput = data.get(2);

        assertEquals(HEADCOUNT.getName(), headcount.getId());
        assertTrue(headcount.isOpen());
        assertEquals(26, headcount.getContent().get(0).size());
        assertEquals(26, headcount.getContent().get(1).size());
        assertEquals("20", headcount.getContent().get(0).get("column_2").getTitle());
        assertEquals(currentTime, headcount.getContent().get(0).get("column_2").getDate()
                .withMinute(0).withSecond(0).withNano(0));
        assertEquals("Cantidad de reps FCST", headcount.getContent().get(0).get("column_2")
                .getTooltip().get("title_2"));
        assertEquals("10", headcount.getContent().get(0).get("column_2")
                .getTooltip().get("subtitle_2"));
        assertEquals("15", headcount.getContent().get(1).get("column_4").getTitle());

        assertEquals(PRODUCTIVITY.getName(), productivity.getId());
        assertFalse(productivity.isOpen());
        assertEquals(26, productivity.getContent().get(0).size());
        assertEquals("30", productivity.getContent().get(0).get("column_3").getTitle());
        assertEquals("Productividad polivalente", productivity.getContent()
                .get(0).get("column_3")
                .getTooltip().get("title_1"));
        assertEquals("20 uds/h", productivity.getContent().get(0).get("column_3")
                .getTooltip().get("subtitle_1"));

        assertEquals(THROUGHPUT.getName(), throughput.getId());
        assertFalse(throughput.isOpen());
        assertFalse(throughput.getContent().isEmpty());
    }

    private EntityRequest createRequest(final EntityType entityType,
                                        final ZonedDateTime currentTime) {
        return createRequest(entityType, currentTime, null);
    }

    private EntityRequest createRequest(final EntityType entityType,
                                        final ZonedDateTime currentTime,
                                        final List<ProcessingType> processingType) {
        return EntityRequest.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .entityType(entityType)
                .processName(List.of(PICKING, PACKING))
                .dateFrom(currentTime)
                .dateTo(currentTime.plusDays(1))
                .processingType(processingType)
                .build();
    }

    private ProductivityRequest createProductivityRequest(final ZonedDateTime currentTime) {
        return ProductivityRequest.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .entityType(PRODUCTIVITY)
                .processName(List.of(PICKING, PACKING))
                .dateFrom(currentTime)
                .dateTo(currentTime.plusDays(1))
                .abilityLevel(List.of(1,2))
                .build();
    }

    private ProjectionRequest createProjectionRequest(final List<Backlog> backlogs,
                                                      final ZonedDateTime currentTime) {
        return ProjectionRequest.builder()
                .processName(List.of(PICKING, PACKING))
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .dateFrom(currentTime)
                .dateTo(currentTime.plusDays(1))
                .type(ProjectionType.CPT)
                .backlog(backlogs)
                .build();
    }

    private List<ProjectionResult> mockProjections(ZonedDateTime utcCurrentTime) {
        return List.of(
                ProjectionResult.builder()
                        .date(CPT_1)
                        .projectedEndDate(utcCurrentTime.plusHours(3).plusMinutes(30))
                        .remainingQuantity(0)
                        .build(),
                ProjectionResult.builder()
                        .date(CPT_2)
                        .projectedEndDate(utcCurrentTime.plusHours(3))
                        .remainingQuantity(0)
                        .build(),
                ProjectionResult.builder()
                        .date(CPT_3)
                        .projectedEndDate(utcCurrentTime.plusHours(3).plusMinutes(25))
                        .remainingQuantity(100)
                        .build(),
                ProjectionResult.builder()
                        .date(CPT_4)
                        .projectedEndDate(utcCurrentTime.plusHours(8).plusMinutes(10))
                        .remainingQuantity(180)
                        .build(),
                ProjectionResult.builder()
                        .date(CPT_5)
                        .projectedEndDate(null)
                        .remainingQuantity(100)
                        .build()
        );
    }

    private List<Backlog> mockBacklog() {
        return List.of(
                Backlog.builder()
                        .date(CPT_1)
                        .quantity(150)
                        .build(),
                Backlog.builder()
                        .date(CPT_2)
                        .quantity(235)
                        .build(),
                Backlog.builder()
                        .date(CPT_3)
                        .quantity(300)
                        .build(),
                Backlog.builder()
                        .date(CPT_4)
                        .quantity(120)
                        .build()
        );
    }

    private List<Backlog> mockSales() {
        return List.of(
                Backlog.builder()
                        .date(CPT_1)
                        .quantity(350)
                        .build(),
                Backlog.builder()
                        .date(CPT_2)
                        .quantity(235)
                        .build(),
                Backlog.builder()
                        .date(CPT_3)
                        .quantity(200)
                        .build(),
                Backlog.builder()
                        .date(CPT_4)
                        .quantity(120)
                        .build()
        );
    }

    private ConfigurationRequest createConfigurationRequest() {
        return  ConfigurationRequest
                .builder()
                .warehouseId(WAREHOUSE_ID)
                .key(PROCESSING_TIME)
                .build();
    }

    private List<Entity> mockHeadcountEntities(final ZonedDateTime utcCurrentTime) {
        return List.of(
                Entity.builder()
                        .date(utcCurrentTime)
                        .processName(PICKING)
                        .source(Source.FORECAST)
                        .value(10)
                        .build(),
                Entity.builder()
                        .date(utcCurrentTime)
                        .processName(PICKING)
                        .source(Source.SIMULATION)
                        .value(20)
                        .build(),
                Entity.builder()
                        .date(utcCurrentTime.plusHours(2))
                        .processName(PACKING)
                        .source(Source.FORECAST)
                        .value(15)
                        .build(),
                Entity.builder()
                        .date(utcCurrentTime.plusDays(1))
                        .processName(PICKING)
                        .source(Source.FORECAST)
                        .value(30)
                        .build()
        );
    }

    private List<Productivity> mockProductivityEntities(final ZonedDateTime utcCurrentTime) {
        return List.of(
                Productivity.builder()
                        .date(utcCurrentTime)
                        .processName(PICKING)
                        .source(Source.FORECAST)
                        .value(60)
                        .abilityLevel(1)
                        .build(),
                Productivity.builder()
                        .date(utcCurrentTime.plusHours(1))
                        .processName(PICKING)
                        .source(Source.SIMULATION)
                        .value(30)
                        .abilityLevel(1)
                        .build(),
                Productivity.builder()
                        .date(utcCurrentTime.plusHours(2))
                        .processName(PICKING)
                        .source(Source.FORECAST)
                        .value(50)
                        .abilityLevel(1)
                        .build(),
                Productivity.builder()
                        .date(utcCurrentTime.plusDays(1))
                        .processName(PICKING)
                        .source(Source.FORECAST)
                        .value(75)
                        .abilityLevel(1)
                        .build(),
                Productivity.builder()
                        .date(utcCurrentTime)
                        .processName(PICKING)
                        .source(Source.FORECAST)
                        .value(50)
                        .abilityLevel(2)
                        .build(),
                Productivity.builder()
                        .date(utcCurrentTime.plusHours(1))
                        .processName(PICKING)
                        .source(Source.SIMULATION)
                        .value(20)
                        .abilityLevel(2)
                        .build(),
                Productivity.builder()
                        .date(utcCurrentTime.plusHours(2))
                        .processName(PICKING)
                        .source(Source.FORECAST)
                        .value(40)
                        .abilityLevel(2)
                        .build(),
                Productivity.builder()
                        .date(utcCurrentTime.plusDays(1))
                        .processName(PICKING)
                        .source(Source.FORECAST)
                        .value(65)
                        .abilityLevel(2)
                        .build()
        );
    }

    private List<Entity> mockThroughputEntities() {
        return new ArrayList<>();
    }

    private Optional<ConfigurationResponse> mockProcessingTimeConfiguration() {
        return Optional.ofNullable(ConfigurationResponse.builder()
                .metricUnit(MINUTES)
                .value(60)
                .build());
    }

    private List<PlanningDistributionResponse> mockPlanningDistribution(
            final ZonedDateTime utcCurrentTime) {
        return List.of(
                new PlanningDistributionResponse(utcCurrentTime, CPT_1, MetricUnit.UNITS, 281),
                new PlanningDistributionResponse(utcCurrentTime, CPT_1, MetricUnit.UNITS, 128),
                new PlanningDistributionResponse(utcCurrentTime, CPT_2, MetricUnit.UNITS, 200),
                new PlanningDistributionResponse(utcCurrentTime, CPT_3, MetricUnit.UNITS, 207),
                new PlanningDistributionResponse(utcCurrentTime, CPT_4, MetricUnit.UNITS, 44),
                new PlanningDistributionResponse(utcCurrentTime, CPT_4, MetricUnit.UNITS, 82),
                new PlanningDistributionResponse(utcCurrentTime, CPT_5, MetricUnit.UNITS, 100)
        );
    }

    private SimpleTable mockSuggestedWaves(final ZonedDateTime utcDateTimeFrom,
                                           final ZonedDateTime utcDateTimeTo) {
        final String title = "Ondas sugeridas";
        final String nextHour = utcDateTimeFrom.withZoneSameInstant(TIME_ZONE.toZoneId())
                .format(ofPattern("HH:mm")) + "-"
                + utcDateTimeTo.withZoneSameInstant(TIME_ZONE.toZoneId())
                .format(ofPattern("HH:mm"));
        final String expextedTitle = "Sig. hora " + nextHour;
        final List<ColumnHeader> columnHeaders = List.of(
                new ColumnHeader("column_1", expextedTitle),
                new ColumnHeader("column_2", "Tamaño de onda")
        );
        final List<Map<String, Object>> data = List.of(
                Map.of("column_1",
                        Map.of("title", "Unidades por onda", "subtitle",
                                MONO_ORDER_DISTRIBUTION.getTitle()),
                        "column_2", "0 uds."
                ),
                Map.of("column_1",
                        Map.of("title", "Unidades por onda", "subtitle",
                                MULTI_BATCH_DISTRIBUTION.getTitle()),
                        "column_2", "100 uds."
                ),
                Map.of("column_1",
                        Map.of("title", "Unidades por onda", "subtitle",
                                MULTI_ORDER_DISTRIBUTION.getTitle()),
                        "column_2", "100 uds."
                )
        );
        return new SimpleTable(title, columnHeaders, data);
    }
}
