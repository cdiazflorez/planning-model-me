package com.mercadolibre.planning.model.me.usecases.wavesuggestion;

import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.ProcessBacklog;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.exception.BacklogGatewayNotSupportedException;
import com.mercadolibre.planning.model.me.gateways.backlog.strategy.BacklogGatewayProvider;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SuggestedWave;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.dto.GetWaveSuggestionInputDto;
import lombok.AllArgsConstructor;

import javax.inject.Named;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MONO_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_BATCH_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo.OUTBOUND_PLANNING;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static java.time.format.DateTimeFormatter.ofPattern;

@Named
@AllArgsConstructor
public class GetWaveSuggestion implements UseCase<GetWaveSuggestionInputDto, SimpleTable> {

    private static final DateTimeFormatter TIME_FORMATTER = ofPattern("HH:mm");
    private final BacklogGatewayProvider backlogGatewayProvider;
    private final PlanningModelGateway planningModelGateway;

    @Override
    public SimpleTable execute(final GetWaveSuggestionInputDto input) {
        final ZonedDateTime currentUtcDateTime = getCurrentUtcDate();
        final ZonedDateTime utcDateTimeFrom = currentUtcDateTime.plusHours(1);
        final ZonedDateTime utcDateTimeTo = currentUtcDateTime.plusHours(2);
        final List<SuggestedWave> suggestedWaves = getSuggestedWaves(input, utcDateTimeFrom,
                utcDateTimeTo);
        return createSimpleTableForWaves(suggestedWaves, input.getZoneId(), utcDateTimeFrom,
                utcDateTimeTo);
    }

    private List<SuggestedWave> getSuggestedWaves(final GetWaveSuggestionInputDto input,
                                                  final ZonedDateTime utcDateTimeFrom,
                                                  final ZonedDateTime utcDateTimeTo) {
        final Integer readyToWaveBacklog =
                backlogGatewayProvider.getBy(input.getWorkflow())
                        .orElseThrow(() -> new BacklogGatewayNotSupportedException(input
                                .getWorkflow()))
                        .getBacklog(List.of(Map.of("status", OUTBOUND_PLANNING.getStatus())),
                                input.getWarehouseId(),
                                utcDateTimeFrom,
                                utcDateTimeFrom.plusHours(25))
                        .stream().findFirst().map(ProcessBacklog::getQuantity).orElse(0);

        return planningModelGateway
                .getSuggestedWaves(input.getWorkflow(),
                        input.getWarehouseId(),
                        utcDateTimeFrom,
                        utcDateTimeTo,
                        readyToWaveBacklog
                );
    }

    private SimpleTable createSimpleTableForWaves(final List<SuggestedWave> suggestedWaves,
                                                  final ZoneId zoneId,
                                                  final ZonedDateTime dateTimeFrom,
                                                  final ZonedDateTime dateTimeTo) {
        final String title = "Ondas sugeridas";

        final ZonedDateTime dateFrom = convertToTimeZone(zoneId, dateTimeFrom);
        final ZonedDateTime dateTo = convertToTimeZone(zoneId, dateTimeTo);
        final String nextHour =
                dateFrom.format(TIME_FORMATTER) + "-" + dateTo.format(TIME_FORMATTER);

        final List<ColumnHeader> columnHeaders = List.of(
                new ColumnHeader("column_1", "Sig. hora " + nextHour, null),
                new ColumnHeader("column_2", "Tamaño de onda", null)
        );
        final List<Map<String, Object>> data = List.of(
                createSuggestedWaveEntry(suggestedWaves, MONO_ORDER_DISTRIBUTION),
                createSuggestedWaveEntry(suggestedWaves, MULTI_BATCH_DISTRIBUTION),
                createSuggestedWaveEntry(suggestedWaves, MULTI_ORDER_DISTRIBUTION)
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

}
