package com.mercadolibre.planning.model.me.clients.rest.backlog;

import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.AREA;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.STEP;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyList;

import com.mercadolibre.planning.model.me.entities.workflows.Area;
import com.mercadolibre.planning.model.me.entities.workflows.BacklogWorkflow;
import com.mercadolibre.planning.model.me.entities.workflows.Step;
import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogPhotoApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogPhotosRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.services.backlog.BacklogRequest;
import com.mercadolibre.planning.model.me.usecases.BacklogPhoto;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Named;
import lombok.AllArgsConstructor;
import lombok.Value;

@Named
@AllArgsConstructor
public class BacklogPhotoApiAdapter implements BacklogPhotoApiGateway {

  private final BacklogApiGateway backlogApiGateway;

  @Override
  public Map<ProcessName, List<BacklogPhoto>> getTotalBacklogPerProcessAndInstantDate(final BacklogRequest request, boolean cached) {
    final List<Photo> photos = cached
        ? backlogApiGateway.getPhotosCached(toBacklogPhotosRequest(request))
        : backlogApiGateway.getPhotos(toBacklogPhotosRequest(request));

    final Instant lastTakenOn = photos.stream().max(Comparator.comparing(Photo::getTakenOn)).map(Photo::getTakenOn).orElse(Instant.now());

    var totalBacklog = photos.stream()
        .filter(photo -> photo.getTakenOn().atZone(UTC).getMinute() == 0 || photo.getTakenOn().equals(lastTakenOn))
        .flatMap(photo -> photo.getGroups()
            .stream()
            .collect(Collectors.toMap(this::mapGroupToProcess, Photo.Group::getTotal, Integer::sum))
            .entrySet()
            .stream()
            .map(entry -> new QuantityAtProcessPhoto(entry.getKey(), photo.getTakenOn(), entry.getValue()))
        ).collect(
            Collectors.groupingBy(
                QuantityAtProcessPhoto::getProcess,
                Collectors.mapping(
                    quantityAtProcessPhoto ->
                        new BacklogPhoto(quantityAtProcessPhoto.getTakenOn(), quantityAtProcessPhoto.getQuantity()),
                    Collectors.toList()
                )
            )
        );

    return request
        .getProcesses().stream().collect(Collectors.toMap(Function.identity(), item -> totalBacklog.getOrDefault(item, emptyList())));
  }

  @Override
  public Map<ProcessName, List<Photo>> getBacklogDetails(final BacklogRequest request) {
    final List<Photo> photos = backlogApiGateway.getPhotos(toBacklogPhotosRequest(request));

    return photos.stream()
        .sorted(Comparator.comparing(Photo::getTakenOn, Comparator.reverseOrder()))
        .filter(photo -> photo.getTakenOn().atZone(UTC).getMinute() == 0 || photos.get(0).equals(photo))
        .map(this::mappingToProcessByPhoto)
        .map(Map::entrySet).flatMap(Set::stream)
        .collect(
            Collectors.groupingBy(
                Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())
            )
        );
  }

  private Map<ProcessName, Photo> mappingToProcessByPhoto(final Photo photo) {
    return photo.getGroups()
        .stream()
        .collect(
            Collectors.groupingBy(this::mapGroupToProcess)
        )
        .entrySet()
        .stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                groupByProcess -> new Photo(photo.getTakenOn(), groupByProcess.getValue())
            )
        );
  }

  private ProcessName mappingToProcess(final Step step, final String area) {
    ProcessName process;
    if (Step.TO_PACK == step) {
      if (Area.PW.getName().equalsIgnoreCase(area)) {
        process = PACKING_WALL;
      } else {
        process = PACKING;
      }
    } else {
      process = ProcessName.getProcessByStep(step);
    }

    return process;
  }

  private BacklogPhotosRequest toBacklogPhotosRequest(final BacklogRequest request) {
    final Set<Step> steps = processesToSteps(request.getProcesses());
    final Set<BacklogWorkflow> workflows = request.getWorkflows()
        .stream()
        .map(Workflow::getBacklogWorkflow)
        .collect(Collectors.toSet());

    return new BacklogPhotosRequest(
        request.getLogisticCenterId(),
        workflows,
        steps,
        request.getDateInFrom(),
        request.getDateInTo(),
        request.getSlaFrom() != null ? request.getSlaFrom().truncatedTo(ChronoUnit.SECONDS) : request.getSlaFrom(),
        request.getSlaTo() != null ? request.getSlaTo().truncatedTo(ChronoUnit.SECONDS) : request.getSlaTo(),
        request.getGroupBy(),
        request.getDateFrom(),
        request.getDateTo()
    );
  }

  private Set<Step> processesToSteps(final Set<ProcessName> processes) {
    return processes.stream()
        .map(ProcessName::getStep)
        .collect(Collectors.toSet());
  }

  private ProcessName mapGroupToProcess(final Photo.Group group) {
    final Step step = group.getGroupValue(STEP).map(Step::valueOf).orElse(null);
    final String area = group.getGroupValue(AREA).orElse(null);
    return mappingToProcess(step, area);
  }

  @Value
  private static class QuantityAtProcessPhoto {
    ProcessName process;

    Instant takenOn;

    int quantity;
  }

}
