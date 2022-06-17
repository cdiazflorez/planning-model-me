package com.mercadolibre.planning.model.me.gateways.backlog;

import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogPhotoRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.usecases.backlog.TotaledBacklogPhoto;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * BacklogPhotoGateway.
 * Es el gateway de los clientes de BA
 * */
public interface BacklogPhotoGateway {
  Map<ProcessName, List<TotaledBacklogPhoto>> getCurrentBacklog(BacklogPhotoRequest request);

  Map<ProcessName, Map<Instant, Integer>> getHistoryBacklog(BacklogPhotoRequest request);
}
