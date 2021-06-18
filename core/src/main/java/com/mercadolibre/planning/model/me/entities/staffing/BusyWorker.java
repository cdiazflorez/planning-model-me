package com.mercadolibre.planning.model.me.entities.staffing;

import lombok.Value;

import java.util.Map;

@Value
public class BusyWorker {

    private Integer total;

    private Map<String, Integer> byArea;
}
