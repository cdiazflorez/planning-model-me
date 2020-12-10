package com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@AllArgsConstructor
@Getter
public class Process {
    private final String title;
    private final List<Metric> metrics;
}
