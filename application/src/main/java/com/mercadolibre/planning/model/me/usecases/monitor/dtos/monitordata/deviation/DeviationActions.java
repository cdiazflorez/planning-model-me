package com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.deviation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class DeviationActions {

    private String applyLabel;
    private String unapplyLabel;
    @JsonInclude
    private DeviationAppliedData appliedData;

}
