package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.services.backlog.BacklogApiAdapter;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogByDateDto;
import lombok.AllArgsConstructor;

import javax.inject.Named;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.DATE_OUT;
import static java.time.ZoneOffset.UTC;
import static java.util.List.of;

@Named
@AllArgsConstructor
public class GetBacklogByDateInbound implements UseCase<GetBacklogByDateDto, List<Backlog>> {

    final BacklogApiAdapter backlogApiAdapter;

    @Override
    public List<Backlog> execute(GetBacklogByDateDto input) {

        final Instant now = Instant.now().truncatedTo(ChronoUnit.MINUTES);

        var consolidations = backlogApiAdapter.getCurrentBacklog(
                now,
                input.getWarehouseId(),
                List.of(input.getWorkflow()),
                List.of(ProcessName.CHECK_IN, ProcessName.PUT_AWAY),
                of(DATE_OUT),
                now.minus(1, ChronoUnit.HOURS),
                now,
                now.minus(7, ChronoUnit.DAYS),
                now.plus(1, ChronoUnit.DAYS)
        );

        var optional = consolidations.stream()
                .max(Comparator.comparing(Consolidation::getDate))
                .map(Consolidation::getDate);

        return optional.map(maxDate -> generateListBacklog(consolidations, maxDate))
                .orElseGet(Collections::emptyList);
    }

    private List<Backlog> generateListBacklog(final List<Consolidation> consolidations,
                                              final Instant maxDate) {
        return consolidations.stream()
                .filter(cons -> cons.getDate().equals(maxDate))
                .collect(Collectors.toMap(collect ->
                                ZonedDateTime.parse(collect
                                                .getKeys()
                                                .get("date_out"))
                                        .withZoneSameInstant(UTC)
                                        .truncatedTo(ChronoUnit.HOURS),
                        Consolidation::getTotal,
                        Integer::sum)).entrySet().stream()
                .map(entry -> new Backlog(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }
}
