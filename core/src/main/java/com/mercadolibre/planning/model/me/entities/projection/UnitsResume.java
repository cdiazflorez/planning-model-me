package com.mercadolibre.planning.model.me.entities.projection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
public class UnitsResume {

    private final int unitCount;
    private final int eventCount;
    private final AnalyticsQueryEvent process;
}
