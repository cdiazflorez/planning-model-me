package com.mercadolibre.planning.model.me.usecases.backlog.dtos;

import com.mercadolibre.planning.model.me.entities.monitor.ProcessBacklogDetail;
import com.mercadolibre.planning.model.me.entities.monitor.ProcessDetail;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

@Value
public class GetBacklogMonitorDetailsResponse {
    private ZonedDateTime currentDatetime;
    private List<ProcessBacklogDetail> dates;
    private ProcessDetail process;
}
