package com.mercadolibre.planning.model.me.entities.staffing;

import java.util.Map;
import lombok.Value;

@Value
public class BusyWorker {

    private Integer total;

    private Map<String, Integer> byArea;
}
