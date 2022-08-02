package com.mercadolibre.planning.model.me.clients.rest.backlog;

import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.AREA;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.STEP;

import com.mercadolibre.planning.model.me.entities.workflows.Area;
import com.mercadolibre.planning.model.me.entities.workflows.BacklogWorkflow;
import com.mercadolibre.planning.model.me.entities.workflows.Step;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogPhotoApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogPhotosRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Process;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.services.backlog.BacklogRequest;
import com.mercadolibre.planning.model.me.usecases.BacklogPhoto;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Named;
import lombok.AllArgsConstructor;
import lombok.Value;

@Named
@AllArgsConstructor
public class BacklogPhotoApiAdapter implements BacklogPhotoApiGateway {

  private final BacklogApiGateway backlogApiGateway;

  @Override
  public Map<Process, List<BacklogPhoto>> getTotalBacklogPerProcessAndInstantDate(final BacklogRequest request) {
    final List<Photo> photos = backlogApiGateway.getPhotos(toBacklogPhotosRequest(request));

    return photos.stream()
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
  }

  @Override
  public Map<Process, List<Photo>> getBacklogDetails(final BacklogRequest request) {
    final List<Photo> photos = backlogApiGateway.getPhotos(toBacklogPhotosRequest(request));

    return photos.stream()
        .map(this::mappingToProcessByPhoto)
        .map(Map::entrySet).flatMap(Set::stream)
        .collect(
            Collectors.groupingBy(
                Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())
            )
        );
  }

  private Map<Process, Photo> mappingToProcessByPhoto(final Photo photo) {
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

  private Process mappingToProcess(final Step step, final String area) {
    Process process;
    if (Step.TO_PACK == step) {
      if (Area.PW.getName().equalsIgnoreCase(area)) {
        process = Process.PACKING_WALL;
      } else {
        process = Process.PACKING;
      }
    } else {
      process = Process.getProcessByStep(step);
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
        request.getSlaFrom(),
        request.getSlaTo(),
        request.getGroupBy(),
        request.getDateFrom(),
        request.getDateTo()
    );
  }

  private Set<Step> processesToSteps(final Set<Process> processes) {
    return processes.stream()
        .map(Process::getStep)
        .collect(Collectors.toSet());
  }

  private Process mapGroupToProcess(final Photo.Group group) {
    final Step step = group.getGroupValue(STEP).map(Step::valueOf).orElse(null);
    final String area = group.getGroupValue(AREA).orElse(null);
    return mappingToProcess(step, area);
  }

  @Value
  private static class QuantityAtProcessPhoto {
    Process process;

    Instant takenOn;

    int quantity;
  }

}
