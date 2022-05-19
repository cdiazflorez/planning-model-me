package com.mercadolibre.planning.model.me.usecases.wavesuggestion;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MONO_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_BATCH_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.OUTBOUND_PLANNING;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDateTime;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.TimeZone.getDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.entities.projection.ProcessBacklog;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.UnitProcessBacklogInput;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.backlog.strategy.BacklogGatewayProvider;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SuggestedWave;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SuggestedWavesRequest;
import com.mercadolibre.planning.model.me.gateways.toogle.FeatureSwitches;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.dto.GetWaveSuggestionInputDto;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetWaveSuggestionTest {

    private static final TimeZone TIME_ZONE = getDefault();

    private static final int NEXT_HOUR_WAVE_SUGGESTION_MINUTES = 40;

    @InjectMocks
    private GetWaveSuggestion getWaveSuggestion;

    @Mock
    private BacklogGatewayProvider backlogGatewayProvider;

    @Mock
    private PlanningModelGateway planningModelGateway;

    @Mock
    private BacklogGateway backlogGateway;

    @Mock
    private LogisticCenterGateway logisticCenterGateway;

    @Mock
    private FeatureSwitches featureSwitches;

    @Mock
    private BacklogApiGateway backlogApiGateway;

    @Test
    void testExecuteCallOutboundUnit() {
        // GIVEN
        final ZonedDateTime currentUtcDateTime = getCurrentUtcDateTime();
        final ZonedDateTime utcDateTimeFrom = getDateFrom(currentUtcDateTime);
        final ZonedDateTime utcDateTimeTo = utcDateTimeFrom.plusHours(1);

        when(featureSwitches.shouldCallBacklogApi()).thenReturn(false);

        mockGateways(utcDateTimeFrom, utcDateTimeTo);
        mockGatewayBacklog(utcDateTimeFrom, false);

        // WHEN
        final SimpleTable simpleTable = getWaveSuggestion.execute(
                GetWaveSuggestionInputDto.builder()
                        .zoneId(TIME_ZONE.toZoneId())
                        .warehouseId(WAREHOUSE_ID)
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(utcDateTimeFrom)
                        .build()
        );

        // THEN
        assertSimpleTable(simpleTable, utcDateTimeFrom, utcDateTimeTo);
        verifyNoInteractions(logisticCenterGateway);
    }

    @Test
    void testExecuteWithoutZoneId() {
        // GIVEN
        final ZonedDateTime currentUtcDateTime = getCurrentUtcDateTime();
        final ZonedDateTime utcDateTimeFrom = getDateFrom(currentUtcDateTime);
        final ZonedDateTime utcDateTimeTo = utcDateTimeFrom.plusHours(1);

        when(featureSwitches.shouldCallBacklogApi()).thenReturn(false);

        mockGateways(utcDateTimeFrom, utcDateTimeTo);
        mockGatewayBacklog(utcDateTimeFrom, false);

        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID)).thenReturn(
                new LogisticCenterConfiguration(TIME_ZONE));

        // WHEN
        final SimpleTable simpleTable = getWaveSuggestion.execute(
                GetWaveSuggestionInputDto.builder()
                        .warehouseId(WAREHOUSE_ID)
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(utcDateTimeFrom)
                        .build()
        );

        // THEN
        assertSimpleTable(simpleTable, utcDateTimeFrom, utcDateTimeTo);
    }

    @Test
    void testExecuteCallBacklogApi() {
        // GIVEN
        final ZonedDateTime currentUtcDateTime = getCurrentUtcDateTime();
        final ZonedDateTime utcDateTimeFrom = getDateFrom(currentUtcDateTime);
        final ZonedDateTime utcDateTimeTo = utcDateTimeFrom.plusHours(1);

        when(featureSwitches.shouldCallBacklogApi()).thenReturn(true);
        mockGatewayBacklog(utcDateTimeFrom, true);

        mockGateways(utcDateTimeFrom, utcDateTimeTo);

        // WHEN
        final SimpleTable simpleTable = getWaveSuggestion.execute(
                GetWaveSuggestionInputDto.builder()
                        .zoneId(TIME_ZONE.toZoneId())
                        .warehouseId(WAREHOUSE_ID)
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(utcDateTimeFrom)
                        .build()
        );

        // THEN
        assertSimpleTable(simpleTable, utcDateTimeFrom, utcDateTimeTo);
        verifyNoInteractions(logisticCenterGateway);
    }

    private void mockGateways(final ZonedDateTime utcDateTimeFrom,
                              final ZonedDateTime utcDateTimeTo) {

        when(planningModelGateway.getSuggestedWaves(
                SuggestedWavesRequest.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .warehouseId(WAREHOUSE_ID)
                        .dateFrom(utcDateTimeFrom)
                        .dateTo(utcDateTimeTo)
                        .backlog(2232)
                        .applyDeviation(true)
                        .build()
        )).thenReturn(mockSuggestedWaveDistribution());
    }

    private void mockGatewayBacklog(final ZonedDateTime utcDateTimeFrom,
                                    final boolean isShouldCallBacklogApi) {

        if (isShouldCallBacklogApi) {
            when(backlogApiGateway.getCurrentBacklog(
                    WAREHOUSE_ID,
                    List.of("outbound-orders"),
                    List.of(OUTBOUND_PLANNING.getStatus()),
                    utcDateTimeFrom.truncatedTo(HOURS).plusHours(1).toInstant(),
                    utcDateTimeFrom.truncatedTo(HOURS).plusHours(26).toInstant(),
                    emptyList()))
                    .thenReturn(List.of(new Consolidation(utcDateTimeFrom.toInstant(), emptyMap(), 2232, true)));
        } else {
            when(backlogGatewayProvider.getBy(FBM_WMS_OUTBOUND))
                    .thenReturn(Optional.of(backlogGateway));

            when(backlogGateway.getUnitBacklog(new UnitProcessBacklogInput(
                    OUTBOUND_PLANNING.getStatus(),
                    WAREHOUSE_ID,
                    utcDateTimeFrom.truncatedTo(HOURS).plusHours(1),
                    utcDateTimeFrom.truncatedTo(HOURS).plusHours(26),
                    null,
                    "order"))
            ).thenReturn(ProcessBacklog.builder()
                    .process(OUTBOUND_PLANNING.getStatus())
                    .quantity(2232)
                    .build());
        }
    }

    private void assertSimpleTable(final SimpleTable simpleTable,
                                   final ZonedDateTime utcDateTimeFrom,
                                   final ZonedDateTime utcDateTimeTo) {
        List<Map<String, Object>> data = simpleTable.getData();
        assertEquals(4, data.size());
        assertEquals("0 uds.", data.get(0).get("column_2"));
        final Map<String, Object> column1Mono = (Map<String, Object>) data.get(0).get("column_1");
        assertEquals(MONO_ORDER_DISTRIBUTION.getTitle(), column1Mono.get("subtitle"));

        assertEquals("100 uds.", data.get(1).get("column_2"));
        final Map<String, Object> column1Multi = (Map<String, Object>) data.get(1).get("column_1");
        assertEquals(MULTI_BATCH_DISTRIBUTION.getTitle(), column1Multi.get("subtitle"));

        assertEquals("100 uds.", data.get(2).get("column_2"));
        final Map<String, Object> column1MultiBatch = (Map<String, Object>) data.get(2).get(
                "column_1");
        assertEquals(MULTI_ORDER_DISTRIBUTION.getTitle(), column1MultiBatch.get("subtitle"));

        assertEquals("200 uds.", data.get(3).get("column_2"));
        final Map<String, Object> column1Total = (Map<String, Object>) data.get(3).get(
                "column_1");
        assertEquals("Total", column1Total.get("title"));

        final String title = simpleTable.getColumns().get(0).getTitle();
        final String nextHour = utcDateTimeFrom.withZoneSameInstant(TIME_ZONE.toZoneId())
                .format(ofPattern("HH:mm")) + "-"
                + utcDateTimeTo.withZoneSameInstant(TIME_ZONE.toZoneId())
                .format(ofPattern("HH:mm"));
        final String expectedTitle = "Sig. hora " + nextHour;
        assertEquals(title, expectedTitle);
    }

    private List<SuggestedWave> mockSuggestedWaveDistribution() {
        return List.of(
                SuggestedWave.builder().quantity(100).waveCardinality(MULTI_BATCH_DISTRIBUTION)
                        .build(),
                SuggestedWave.builder().quantity(100).waveCardinality(MULTI_ORDER_DISTRIBUTION)
                        .build()
        );
    }

    private ZonedDateTime getDateFrom(final ZonedDateTime now) {
        return now.getMinute() < NEXT_HOUR_WAVE_SUGGESTION_MINUTES
                ? now.truncatedTo(HOURS)
                : now.truncatedTo(HOURS).plusHours(1);
    }
}
