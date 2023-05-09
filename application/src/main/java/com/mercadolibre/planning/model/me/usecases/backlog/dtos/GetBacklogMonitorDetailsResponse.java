package com.mercadolibre.planning.model.me.usecases.backlog.dtos;

import com.mercadolibre.planning.model.me.entities.monitor.DetailedBacklogPhoto;
import com.mercadolibre.planning.model.me.entities.monitor.ProcessDetail;
import java.time.Instant;
import java.util.List;
import lombok.Value;

@Value
public class GetBacklogMonitorDetailsResponse {
    private Instant currentDatetime;
    private List<DetailedBacklogPhoto> dates;
    private ProcessDetail process;
}
