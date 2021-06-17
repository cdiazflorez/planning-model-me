package com.mercadolibre.planning.model.me.entities.staffing;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class Process {

    private String process;

    private Integer netProductivity;

    private Integer targetProductivity;

    private Integer throughput;

    private Worker workers;
}
