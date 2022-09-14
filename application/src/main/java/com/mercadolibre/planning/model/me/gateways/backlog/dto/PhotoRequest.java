package com.mercadolibre.planning.model.me.gateways.backlog.dto;

import static com.mercadolibre.planning.model.me.gateways.backlog.dto.FilterBacklog.GROUP_BY;
import static com.mercadolibre.planning.model.me.gateways.backlog.dto.FilterBacklog.STEPS;
import static com.mercadolibre.planning.model.me.gateways.backlog.dto.FilterBacklog.WORKFLOWS;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;

import com.mercadolibre.planning.model.me.entities.workflows.BacklogWorkflow;
import com.mercadolibre.planning.model.me.entities.workflows.Step;
import com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
@AllArgsConstructor
public class PhotoRequest {
  protected String logisticCenterId;

  protected Set<BacklogWorkflow> workflows;

  protected Set<Step> steps;

  protected Instant dateInFrom;

  protected Instant dateInTo;

  protected Instant slaFrom;

  protected Instant slaTo;

  protected Set<BacklogGrouper> groupBy;


  /**
   * Get query params photo override in child classes.
   *
   * @return params in a map
   */
  public Map<String, String> getQueryParamsPhoto() {
    return null;
  }

  protected void addAsQueryParam(Map<String, String> map, String key, List<String> value) {
    if (value != null) {
      map.put(key, String.join(",", value));
    }
  }

  protected void addAsQueryParam(Map<String, String> map, String key, Instant value) {
    if (value != null) {
      map.put(key, ISO_INSTANT.format(value));
    }
  }

  protected Map<String, List<String>> generateMapOfParameters() {
    var listWorkflows = Optional.ofNullable(workflows)
        .map(workflowsList -> workflowsList.stream()
            .map(BacklogWorkflow::getName)
            .collect(Collectors.toList())
        ).orElse(null);

    var listSteps = Optional.ofNullable(steps)
        .map(stepList -> stepList.stream()
            .map(Step::getName)
            .collect(Collectors.toList())
        ).orElse(null);

    var listGroups = Optional.ofNullable(groupBy)
        .map(stepList -> stepList.stream()
            .map(BacklogGrouper::getName)
            .collect(Collectors.toList())
        ).orElse(null);

    return Map.of(
        WORKFLOWS.getName(), listWorkflows,
        STEPS.getName(), listSteps,
        GROUP_BY.getName(), listGroups
    );
  }
}
