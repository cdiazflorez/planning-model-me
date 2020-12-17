package com.mercadolibre.planning.model.me.clients.rest.analytics.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UnitsResumeResponse {

    private int unitCount;
    private int eventCount;
    private String process;
}
