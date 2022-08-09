package com.mercadolibre.planning.model.me.gateways.backlog.dto;

import static com.mercadolibre.planning.model.me.gateways.backlog.dto.FilterBacklog.DATE_IN_FROM;
import static com.mercadolibre.planning.model.me.gateways.backlog.dto.FilterBacklog.DATE_IN_TO;
import static com.mercadolibre.planning.model.me.gateways.backlog.dto.FilterBacklog.DATE_OUT_FROM;
import static com.mercadolibre.planning.model.me.gateways.backlog.dto.FilterBacklog.DATE_OUT_TO;
import static com.mercadolibre.planning.model.me.gateways.backlog.dto.FilterBacklog.GROUP_BY;
import static com.mercadolibre.planning.model.me.gateways.backlog.dto.FilterBacklog.LOGISTIC_CENTER_ID;
import static com.mercadolibre.planning.model.me.gateways.backlog.dto.FilterBacklog.PHOTO_DATE_TO;
import static com.mercadolibre.planning.model.me.gateways.backlog.dto.FilterBacklog.STEPS;
import static com.mercadolibre.planning.model.me.gateways.backlog.dto.FilterBacklog.WORKFLOWS;

import com.mercadolibre.planning.model.me.entities.workflows.BacklogWorkflow;
import com.mercadolibre.planning.model.me.entities.workflows.Step;
import com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BacklogLastPhotoRequest extends PhotoRequest {

  Instant photoDateTo;

  public BacklogLastPhotoRequest(final String logisticCenterId,
                                 final Set<BacklogWorkflow> workflows,
                                 final Set<Step> steps,
                                 final Instant dateInFrom,
                                 final Instant dateInTo,
                                 final Instant slaFrom,
                                 final Instant slaTo,
                                 final Set<BacklogGrouper> groupBy,
                                 final Instant photoDateTo) {
    super(logisticCenterId, workflows, steps, dateInFrom, dateInTo, slaFrom, slaTo, groupBy);
    this.photoDateTo = photoDateTo;
  }

  /**
   * Get query params last photo.
   *
   * @return params in a map
   */
  public Map<String, String> getQueryParamsPhoto() {
    final Map<String, List<String>> parametersByType = generateMapOfParameters();
    final Map<String, String> params = new HashMap<>();

    params.put(LOGISTIC_CENTER_ID.getName(), logisticCenterId);
    addAsQueryParam(params, PHOTO_DATE_TO.getName(), photoDateTo);
    addAsQueryParam(params, WORKFLOWS.getName(), parametersByType.get(WORKFLOWS.getName()));
    addAsQueryParam(params, STEPS.getName(), parametersByType.get(STEPS.getName()));
    addAsQueryParam(params, DATE_OUT_FROM.getName(), slaFrom);
    addAsQueryParam(params, DATE_OUT_TO.getName(), slaTo);
    addAsQueryParam(params, DATE_IN_FROM.getName(), dateInFrom);
    addAsQueryParam(params, DATE_IN_TO.getName(), dateInTo);
    addAsQueryParam(params, GROUP_BY.getName(), parametersByType.get(GROUP_BY.getName()));

    return params;
  }
}
