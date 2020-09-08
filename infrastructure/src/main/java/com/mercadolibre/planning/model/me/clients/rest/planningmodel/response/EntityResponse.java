package com.mercadolibre.planning.model.me.clients.rest.planningmodel.response;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EntityResponse {

    private String date;

    private String workflow;

    private String processName;

    private int value;

    private String source;

    private String metricUnit;
}
