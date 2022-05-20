package com.mercadolibre.planning.model.me.gateways.backlog.dto;


import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
@Getter
@AllArgsConstructor
public class BacklogCurrentRequest {
    private Instant requestDate;

    private String warehouseId;

    private List<String> workflows;

    private List<String> processes;

    private List<String> steps;

    private List<String> groupingFields;

    private Instant dateInFrom;

    private Instant dateInTo;

    private Instant slaFrom;

    private Instant slaTo;

    public BacklogCurrentRequest(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public BacklogCurrentRequest(final Instant requestDate,
                                 final String warehouseId,
                                 final List<String> workflows,
                                 final List<String> processes,
                                 final List<String> steps,
                                 final List<String> groupingFields,
                                 final Instant slaFrom,
                                 final Instant slaTo) {
        this.requestDate = requestDate;
        this.warehouseId = warehouseId;
        this.workflows = workflows;
        this.processes = processes;
        this.steps = steps;
        this.groupingFields = groupingFields;
        this.slaFrom = slaFrom;
        this.slaTo = slaTo;
    }

    public BacklogCurrentRequest withDateInRange(Instant from, Instant to) {
        this.dateInFrom = from;
        this.dateInTo = to;
        return this;
    }

    public BacklogCurrentRequest withWorkflows(List<String> workflows) {
        this.workflows = workflows;
        return this;
    }

    public BacklogCurrentRequest withSteps(List<String> steps) {
        this.steps = steps;
        return this;
    }

    public BacklogCurrentRequest withGroupingFields(List<String> groupingFields) {
        this.groupingFields = groupingFields;
        return this;
    }
}
