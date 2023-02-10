package com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.deviation;

import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class DeviationAppliedData {

    private String title;
    private String icon;
    private DeviationValues values;
    private ZonedDateTime dateFrom;
    private ZonedDateTime dateTo;
}
