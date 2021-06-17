package com.mercadolibre.planning.model.me.entities.staffing;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;

@Getter
@ToString
@EqualsAndHashCode
public class Worker {

    private Integer idle;

    private BusyWorker busy;

    public Worker(final Integer idle, final Integer busy, final Map<String, Integer> byArea) {
        this.idle = idle;
        this.busy = new BusyWorker(busy, byArea);
    }

    public Worker(final Integer idle, final Integer busy) {
        this.idle = idle;
        this.busy = new BusyWorker(busy, null);
    }
}
