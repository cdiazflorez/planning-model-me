package com.mercadolibre.planning.model.me.entities.projection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Builder
@Getter
@Setter
public class UnitsResume {

    private int unitCount;
    private int eventCount;
    private AnalyticsQueryEvent process;
}
