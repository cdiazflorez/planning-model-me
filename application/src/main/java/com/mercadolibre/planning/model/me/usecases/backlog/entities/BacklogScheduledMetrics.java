package com.mercadolibre.planning.model.me.usecases.backlog.entities;

import com.mercadolibre.planning.model.me.gateways.backlog.dto.Indicator;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BacklogScheduledMetrics {
    Indicator expected;
    Indicator received;
    Indicator deviation;
}
