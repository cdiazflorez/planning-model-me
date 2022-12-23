package com.mercadolibre.planning.model.me.usecases.backlog;

import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.DATE_OUT;
import static java.time.ZoneOffset.UTC;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.workflows.BacklogWorkflow;
import com.mercadolibre.planning.model.me.entities.workflows.Step;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogLastPhotoRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogByDateDto;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Named;
import lombok.AllArgsConstructor;

@Named
@AllArgsConstructor
public class GetBacklogByDateInbound implements UseCase<GetBacklogByDateDto, List<Backlog>> {

  private static final int MINUS_HOURS = 1;

  private static final int MINUS_DAYS = 30;

  private final BacklogApiGateway backlogApiGateway;

  @Override
  public List<Backlog> execute(final GetBacklogByDateDto input) {
    final Instant now = Instant.now().truncatedTo(ChronoUnit.MINUTES);

    var photo = backlogApiGateway.getLastPhoto(new BacklogLastPhotoRequest(
        input.getWarehouseId(),
        Set.of(BacklogWorkflow.INBOUND, BacklogWorkflow.INBOUND_TRANSFER),
        Set.of(Step.CHECK_IN, Step.PUT_AWAY),
        null,
        null,
        now.minus(MINUS_DAYS, ChronoUnit.DAYS),
        input.getDateTo(),
        Set.of(DATE_OUT),
        now
    ));


    return photo == null
        ? Collections.emptyList()
        : generateListBacklog(photo.getGroups());
  }

  private List<Backlog> generateListBacklog(final List<Photo.Group> groups) {
    return groups.stream()
        .collect(
            Collectors.toMap(collect ->
                    ZonedDateTime.parse(collect
                            .getKey()
                            .get(DATE_OUT))
                        .withZoneSameInstant(UTC)
                        .truncatedTo(ChronoUnit.HOURS),
                Photo.Group::getTotal,
                Integer::sum))
        .entrySet()
        .stream()
        .map(entry -> new Backlog(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
  }
}
