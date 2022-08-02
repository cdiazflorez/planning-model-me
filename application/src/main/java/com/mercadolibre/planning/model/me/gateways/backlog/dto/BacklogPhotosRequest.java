package com.mercadolibre.planning.model.me.gateways.backlog.dto;

import static com.mercadolibre.planning.model.me.gateways.backlog.dto.FilterBacklog.DATE_FROM;
import static com.mercadolibre.planning.model.me.gateways.backlog.dto.FilterBacklog.DATE_IN_FROM;
import static com.mercadolibre.planning.model.me.gateways.backlog.dto.FilterBacklog.DATE_IN_TO;
import static com.mercadolibre.planning.model.me.gateways.backlog.dto.FilterBacklog.DATE_OUT_FROM;
import static com.mercadolibre.planning.model.me.gateways.backlog.dto.FilterBacklog.DATE_OUT_TO;
import static com.mercadolibre.planning.model.me.gateways.backlog.dto.FilterBacklog.DATE_TO;
import static com.mercadolibre.planning.model.me.gateways.backlog.dto.FilterBacklog.GROUP_BY;
import static com.mercadolibre.planning.model.me.gateways.backlog.dto.FilterBacklog.LOGISTIC_CENTER_ID;
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

public class BacklogPhotosRequest extends PhotoRequest {

  Instant dateFrom;

  Instant dateTo;

  /**
   * Constructor BacklogPhotosRequest.
   * @param logisticCenterId logisticCenter
   * @param workflows set of workflows
   * @param steps set of workflows
   * @param dateInFrom date in from
   * @param dateInTo date in to
   * @param slaFrom sla from
   * @param slaTo sla to
   * @param groupBy group by
   * @param dateFrom date from
   * @param dateTo date to
   * */
  public BacklogPhotosRequest(final String logisticCenterId,
                              final Set<BacklogWorkflow> workflows,
                              final Set<Step> steps,
                              final Instant dateInFrom,
                              final Instant dateInTo,
                              final Instant slaFrom,
                              final Instant slaTo,
                              final Set<BacklogGrouper> groupBy,
                              final Instant dateFrom,
                              final Instant dateTo) {
    super(logisticCenterId, workflows, steps, dateInFrom, dateInTo, slaFrom, slaTo, groupBy);
    this.dateFrom = dateFrom;
    this.dateTo = dateTo;
  }

  /**
   * Get query params photos.
   *
   * @return params in a map
   */
  public Map<String, String> getQueryParamsPhoto() {
    final Map<String, String> params = new HashMap<>();

    final Map<String, List<String>> parametersByType = generateMapOfParameters();

    params.put(LOGISTIC_CENTER_ID.getName(), logisticCenterId);
    addAsQueryParam(params, WORKFLOWS.getName(), parametersByType.get(WORKFLOWS.getName()));
    addAsQueryParam(params, STEPS.getName(), parametersByType.get(STEPS.getName()));
    addAsQueryParam(params, DATE_FROM.getName(), dateFrom);
    addAsQueryParam(params, DATE_TO.getName(), dateTo);
    addAsQueryParam(params, DATE_IN_FROM.getName(), dateInFrom);
    addAsQueryParam(params, DATE_IN_TO.getName(), dateInTo);
    addAsQueryParam(params, DATE_OUT_FROM.getName(), slaFrom);
    addAsQueryParam(params, DATE_OUT_TO.getName(), slaTo);
    addAsQueryParam(params, GROUP_BY.getName(), parametersByType.get(GROUP_BY.getName()));

    return params;
  }
}
