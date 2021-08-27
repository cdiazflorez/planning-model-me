package com.mercadolibre.planning.model.me.entities.monitor;

import lombok.Value;

import java.util.List;

@Value
public class ProcessDetail {
    private String process;
    private UnitMeasure total;
    private List<BacklogByDate> backlogs;
}
