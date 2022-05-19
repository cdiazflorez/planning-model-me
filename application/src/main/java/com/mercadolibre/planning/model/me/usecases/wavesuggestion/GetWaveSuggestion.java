package com.mercadolibre.planning.model.me.usecases.wavesuggestion;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MONO_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_BATCH_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.OUTBOUND_PLANNING;
import static com.mercadolibre.planning.model.me.utils.DateUtils.HOUR_MINUTES_FORMATTER;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.emptyList;

import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.exception.BacklogGatewayNotSupportedException;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.UnitProcessBacklogInput;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.backlog.strategy.BacklogGatewayProvider;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SuggestedWave;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SuggestedWavesRequest;
import com.mercadolibre.planning.model.me.gateways.toogle.FeatureSwitches;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.dto.GetWaveSuggestionInputDto;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import javax.inject.Named;
import lombok.AllArgsConstructor;

@Named
@AllArgsConstructor
public class GetWaveSuggestion implements UseCase<GetWaveSuggestionInputDto, SimpleTable> {

    private static final int NEXT_HOUR_WAVE_SUGGESTION_MINUTES = 40;

    private static final int TO_HOURS = 25;

    private final BacklogGatewayProvider backlogGatewayProvider;

    private final PlanningModelGateway planningModelGateway;

    protected final LogisticCenterGateway logisticCenterGateway;

    private final FeatureSwitches featureSwitches;

    private final BacklogApiGateway backlogGateway;

    @Override
    public SimpleTable execute(final GetWaveSuggestionInputDto input) {

        final ZonedDateTime now = input.getDate() == null ? getCurrentUtcDate() : input.getDate();
        final ZonedDateTime suggestionTimeFrom = getDateFrom(now);
        final ZonedDateTime suggestionTimeTo = suggestionTimeFrom.plusHours(1);

        final List<SuggestedWave> suggestedWaves = getSuggestedWaves(input, now, suggestionTimeTo);

        return createSimpleTableForWaves(
                suggestedWaves,
                getZoneId(input),
                suggestionTimeFrom,
                suggestionTimeTo
        );
    }

    private ZonedDateTime getDateFrom(final ZonedDateTime now) {
        return now.getMinute() < NEXT_HOUR_WAVE_SUGGESTION_MINUTES
                ? now.truncatedTo(HOURS)
                : now.truncatedTo(HOURS).plusHours(1);
    }

    private List<SuggestedWave> getSuggestedWaves(final GetWaveSuggestionInputDto input,
                                                  final ZonedDateTime now,
                                                  final ZonedDateTime dateTo) {

        final ZonedDateTime cptFrom = now.truncatedTo(HOURS).plusHours(1);
        final int readyToWaveBacklog;

        if (featureSwitches.shouldCallBacklogApi()) {
            final List<Consolidation> backlogConsolidation = backlogGateway.getCurrentBacklog(
                    input.getWarehouseId(),
                    List.of("outbound-orders"),
                    List.of(OUTBOUND_PLANNING.getStatus()),
                    cptFrom.toInstant(),
                    cptFrom.plusHours(TO_HOURS).toInstant(),
                    emptyList()
            );
            readyToWaveBacklog = backlogConsolidation.stream().mapToInt(Consolidation::getTotal).sum();

        } else {
            readyToWaveBacklog = backlogGatewayProvider
                    .getBy(input.getWorkflow())
                    .orElseThrow(() -> new BacklogGatewayNotSupportedException(input.getWorkflow()))
                    .getUnitBacklog(new UnitProcessBacklogInput(OUTBOUND_PLANNING.getStatus(), input.getWarehouseId(),
                            cptFrom, cptFrom.plusHours(TO_HOURS), null, "order")).getQuantity();
        }

        return planningModelGateway.getSuggestedWaves(
                SuggestedWavesRequest.builder()
                        .workflow(input.getWorkflow())
                        .warehouseId(input.getWarehouseId())
                        .dateFrom(now)
                        .dateTo(dateTo)
                        .backlog(readyToWaveBacklog)
                        .applyDeviation(true)
                        .build()
        );
    }

    private SimpleTable createSimpleTableForWaves(final List<SuggestedWave> suggestedWaves,
                                                  final ZoneId zoneId,
                                                  final ZonedDateTime dateTimeFrom,
                                                  final ZonedDateTime dateTimeTo) {
        final String title = "Ondas sugeridas";

        final ZonedDateTime dateFrom = convertToTimeZone(zoneId, dateTimeFrom);
        final ZonedDateTime dateTo = convertToTimeZone(zoneId, dateTimeTo);
        final String nextHour = dateFrom.format(HOUR_MINUTES_FORMATTER)
                + "-" + dateTo.format(HOUR_MINUTES_FORMATTER);

        final List<ColumnHeader> columnHeaders = List.of(
                new ColumnHeader("column_1", "Sig. hora " + nextHour, null),
                new ColumnHeader("column_2", "Tamaño de onda", null)
        );
        final List<Map<String, Object>> data = List.of(
                createSuggestedWaveEntry(suggestedWaves, MONO_ORDER_DISTRIBUTION),
                createSuggestedWaveEntry(suggestedWaves, MULTI_BATCH_DISTRIBUTION),
                createSuggestedWaveEntry(suggestedWaves, MULTI_ORDER_DISTRIBUTION),
                createTotalEntry(suggestedWaves)
        );
        return new SimpleTable(title, columnHeaders, data);
    }

    private Map<String, Object> createSuggestedWaveEntry(final List<SuggestedWave> suggestedWaves,
                                                         final Cardinality cardinality) {
        return Map.of("column_1",
                Map.of("title", "Unidades por onda", "subtitle",
                        cardinality.getTitle()),
                "column_2", suggestedWaves.stream()
                        .filter(suggestedWave -> suggestedWave
                                .getWaveCardinality()
                                .equals(cardinality))
                        .findAny()
                        .map(SuggestedWave::getQuantity)
                        .orElse(0) + " uds."
        );
    }

    private Map<String, Object> createTotalEntry(final List<SuggestedWave> suggestedWaves) {
        return Map.of(
                "column_1", Map.of("title", "Total"),
                "column_2", suggestedWaves.stream()
                        .mapToInt(SuggestedWave::getQuantity)
                        .sum() + " uds."
        );
    }

    private ZoneId getZoneId(final GetWaveSuggestionInputDto input) {
        if (input.getZoneId() == null) {
            return logisticCenterGateway.getConfiguration(input.getWarehouseId()).getZoneId();
        }
        return input.getZoneId();
    }
}
