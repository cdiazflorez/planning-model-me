package com.mercadolibre.planning.model.me.gateways.backlog.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@EqualsAndHashCode
@Builder
@Getter
@AllArgsConstructor
public class BacklogRequest {
    private Instant requestDate;
    private String warehouseId;
    private List<String> workflows;
    private List<String> processes;
    private List<String> steps;
    private List<String> groupingFields;
    private Instant dateFrom;
    private Instant dateTo;
    private Instant dateInFrom;
    private Instant dateInTo;
    private Instant slaFrom;
    private Instant slaTo;

    public BacklogRequest(Instant requestDate, String warehouseId, List<String> workflows, List<String> processes,
                          List<String> steps, List<String> groupingFields, Instant dateFrom, Instant dateTo,
                          Instant slaFrom, Instant slaTo) {
        this.requestDate = requestDate;
        this.warehouseId = warehouseId;
        this.workflows = workflows;
        this.processes = processes;
        this.steps = steps;
        this.groupingFields = groupingFields;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.dateInFrom = null;
        this.dateInTo = null;
        this.slaFrom = slaFrom;
        this.slaTo = slaTo;
    }

    public BacklogRequest(String warehouseId, Instant dateFrom, Instant dateTo) {
        this.warehouseId = warehouseId;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
    }

    public BacklogRequest withSlaRange(Instant from, Instant to) {
        this.slaFrom = from;
        this.slaTo = slaTo;
        return this;
    }

    public BacklogRequest withDateInRange(Instant from, Instant to) {
        this.dateInFrom = from;
        this.dateInTo = to;
        return this;
    }

    public BacklogRequest withRequestDate(Instant requestDate) {
        this.requestDate = requestDate;
        return this;
    }

    public BacklogRequest withWorkflows(List<String> workflows) {
        this.workflows = workflows;
        return this;
    }

    public BacklogRequest withProcesses(List<String> processes) {
        this.processes = processes;
        return this;
    }

    public BacklogRequest withSteps(List<String> steps) {
        this.steps = steps;
        return this;
    }

    public BacklogRequest withGroupingFields(List<String> groupingFields) {
        this.groupingFields = groupingFields;
        return this;
    }

}
