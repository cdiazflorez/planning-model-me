package com.mercadolibre.planning.model.me.gateways.backlog;

import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogCurrentRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogLastPhotoRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogPhotosRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo;
import java.time.Instant;
import java.util.List;

/**
 * Interface of backlogApiClient.
 * gateway de los clients de BA
 */
public interface BacklogApiGateway {

  List<Consolidation> getBacklog(BacklogRequest request);

  List<Consolidation> getCurrentBacklog(String logisticCenterId,
                                        List<String> workflows,
                                        List<String> steps,
                                        Instant slaFrom,
                                        Instant slaTo,
                                        List<String> groupingFields);

  List<Consolidation> getCurrentBacklog(BacklogCurrentRequest request);

  List<Photo> getPhotos(BacklogPhotosRequest request);

  Photo getLastPhoto(BacklogLastPhotoRequest request);
}
