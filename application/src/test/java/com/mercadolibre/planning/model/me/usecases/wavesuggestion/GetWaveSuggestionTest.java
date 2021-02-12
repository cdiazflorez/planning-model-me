package com.mercadolibre.planning.model.me.usecases.wavesuggestion;

import com.mercadolibre.planning.model.me.entities.projection.ProcessBacklog;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.strategy.BacklogGatewayProvider;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SuggestedWave;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SuggestedWavesRequest;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.dto.GetWaveSuggestionInputDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MONO_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_BATCH_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo.OUTBOUND_PLANNING;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.TimeZone.getDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetWaveSuggestionTest {

    private static final TimeZone TIME_ZONE = getDefault();

    @InjectMocks
    private GetWaveSuggestion getWaveSuggestion;

    @Mock
    private BacklogGatewayProvider backlogGatewayProvider;

    @Mock
    private PlanningModelGateway planningModelGateway;

    @Mock
    private BacklogGateway backlogGateway;

    @Test
    void testExecute() {
        final ZonedDateTime currentUtcDateTime = getCurrentUtcDate();
        final ZonedDateTime utcDateTimeFrom = currentUtcDateTime.plusHours(1);
        final ZonedDateTime utcDateTimeTo = currentUtcDateTime.plusHours(2);

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

        when(backlogGatewayProvider.getBy(FBM_WMS_OUTBOUND))
                .thenReturn(Optional.of(backlogGateway));
        when(backlogGateway.getBacklog(List.of(Map.of("status", OUTBOUND_PLANNING.getStatus())),
                WAREHOUSE_ID,
                utcDateTimeFrom,
                utcDateTimeFrom.plusHours(25)
        )).thenReturn(List.of(
                ProcessBacklog.builder()
                        .process(OUTBOUND_PLANNING.getStatus())
                        .quantity(2232)
                        .build()
        ));

        final SimpleTable simpleTable = getWaveSuggestion.execute(
                GetWaveSuggestionInputDto.builder()
                .zoneId(TIME_ZONE.toZoneId())
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .build()
        );

        assertSimpleTable(simpleTable, utcDateTimeFrom, utcDateTimeTo);
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

    private List<SuggestedWave> mockSuggestedWaveDistribution() {
        return List.of(
                SuggestedWave.builder().quantity(100).waveCardinality(MULTI_BATCH_DISTRIBUTION)
                        .build(),
                SuggestedWave.builder().quantity(100).waveCardinality(MULTI_ORDER_DISTRIBUTION)
                        .build()
        );
    }
}
