package com.mercadolibre.planning.model.me.usecases.backlog;

import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.DATE_OUT;
import static java.time.ZoneOffset.UTC;
import static java.util.List.of;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.services.backlog.BacklogApiAdapter;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogByDateDto;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Named;
import lombok.AllArgsConstructor;

@Named
@AllArgsConstructor
public class GetBacklogByDateInbound implements UseCase<GetBacklogByDateDto, List<Backlog>> {

  private static final int MINUS_HOURS = 1;

  private static final int MINUS_DAYS = 7;

  final BacklogApiAdapter backlogApiAdapter;

  @Override
  public List<Backlog> execute(final GetBacklogByDateDto input) {
    final Instant now = Instant.now().truncatedTo(ChronoUnit.MINUTES);

    var consolidations = backlogApiAdapter.getCurrentBacklog(
        now,
        input.getWarehouseId(),
        of(input.getWorkflow()),
        of(ProcessName.CHECK_IN, ProcessName.PUT_AWAY),
        of(DATE_OUT),
        now.minus(MINUS_HOURS, ChronoUnit.HOURS),
        now,
        now.minus(MINUS_DAYS, ChronoUnit.DAYS),
        input.getDateTo()
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
