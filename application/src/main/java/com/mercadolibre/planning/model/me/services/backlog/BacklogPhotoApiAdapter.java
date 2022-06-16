package com.mercadolibre.planning.model.me.services.backlog;

import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogPhotoGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogPhotoRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.usecases.backlog.TotaledBacklogPhoto;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class BacklogPhotoApiAdapter implements BacklogPhotoGateway {

  private static final String STATUS = "status";

  private static final Map<String, String> STATUS_BY_PROCESS = Map.of(
      "PENDING", "waving",
      "TO_PICK", "picking",
      "TO_PACK", "packing",
      "CHECK_IN", "check_in",
      "PUT_AWAY", "put_away",
      "SCHEDULED", "scheduled"
  );

  private final BacklogApiGateway backlogApiGateway;

  @Override
  public Map<ProcessName, List<TotaledBacklogPhoto>> getCurrentBacklog(final BacklogPhotoRequest request) {
    request.setProcesses(formatterStatusToProcess(request.getProcesses()));
    final List<Photo> photos = backlogApiGateway.getPhotos(request);

    return totaledBacklogByProcess(photos);
  }

  @Override
  public Map<ProcessName, Map<Instant, Integer>> getHistoryBacklog(final BacklogPhotoRequest request) {
    request.setProcesses(formatterStatusToProcess(request.getProcesses()));
    final List<Photo> photos = backlogApiGateway.getPhotos(request);

    return totaledBacklogByProcess(photos)
        .entrySet().stream().collect(Collectors
            .toMap(
                Map.Entry::getKey,
                x -> x.getValue().stream()
                    .collect(
                        Collectors
                            .toMap(TotaledBacklogPhoto::getTakenOn, TotaledBacklogPhoto::getQuantity)
                    )
            )
        );
  }

  private List<String> formatterStatusToProcess(final List<String> status) {
    return status.stream()
        .map(statusName ->
            STATUS_BY_PROCESS.values().stream()
                .filter(
                    sp -> sp.equals(statusName)
                ).findFirst()
                .orElse(null)
        ).collect(Collectors.toList());
  }

  private Map<ProcessName, List<TotaledBacklogPhoto>> totaledBacklogByProcess(final List<Photo> photos) {
    var streamOfBacklogByProcess = photos.stream()
        .map(
            photo -> photo.getGroups().stream()
                .collect(Collectors
                    .toMap(
                        group -> ProcessName.from(STATUS_BY_PROCESS.get(group.getKey().get(STATUS))),
                        group -> List.of(new TotaledBacklogPhoto(photo.getTakenOn(), group.getTotal()))
                    )
                )
        );

    return streamOfBacklogByProcess
        .flatMap(stBacklogByProcess -> stBacklogByProcess.entrySet().stream())
        .collect(Collectors
            .toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (totaledBacklog1, totaledBacklog2) -> List.of(totaledBacklog1.get(0), totaledBacklog2.get(0))
            )
        );
  }

}
