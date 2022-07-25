package com.mercadolibre.planning.model.me.gateways.backlog;

import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Process;
import com.mercadolibre.planning.model.me.services.backlog.BacklogRequest;
import com.mercadolibre.planning.model.me.usecases.BacklogPhoto;
import java.util.List;
import java.util.Map;

/**
 * Gateway of backlogApi.
 */
public interface BacklogPhotoApiGateway {

  /**
   * Get backlog of summary.
   * Gets the backlog of photo in the interval specified by the {@link BacklogRequest}.
   * The cells of the returned photos are filtered and grouped according to the {@link BacklogRequest} parameter.
   *
   * @param request parameter for call client of backlog api
   * @return map of process by list of BacklogPhoto
   */
  Map<Process, List<BacklogPhoto>> getTotalBacklogPerProcessAndInstantDate(BacklogRequest request);

  /**
   * Get backlog details.
   * Gets the backlog photos in the interval specified by the  {@link BacklogRequest}.
   * The cells of the returned photos are filtered and grouped according to the {@link BacklogRequest} parameter.
   *
   * @param request parameter for call client of backlog api
   * @return map of process by backlog like "takenOn, total"
   */
  Map<Process, List<Photo>> getBacklogDetails(BacklogRequest request);


}
