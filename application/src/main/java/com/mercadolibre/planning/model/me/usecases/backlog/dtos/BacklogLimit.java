package com.mercadolibre.planning.model.me.usecases.backlog.dtos;

import lombok.Value;

@Value
public class BacklogLimit {
    int min;
    int max;
}
