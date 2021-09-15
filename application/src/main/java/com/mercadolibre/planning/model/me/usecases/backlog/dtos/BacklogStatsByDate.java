package com.mercadolibre.planning.model.me.usecases.backlog.dtos;

import lombok.Value;

import java.time.ZonedDateTime;

@Value
public class BacklogStatsByDate {
    ZonedDateTime date;
    Integer units;
    Integer throughput;
    Integer historical;
}
