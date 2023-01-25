package com.mercadolibre.planning.model.me.gateways.backlog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ScheduleBacklogResponse {
    @JsonProperty("request_date")
    Instant viewDate;
    @JsonProperty("deviations_applied")
    List<ScheduleAdjustment> scheduleAdjustments;
    @JsonProperty("scheduled")
    Map<String, BacklogScheduled> backlogScheduled;
}
