package com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos;

import lombok.Value;

import java.time.ZoneId;
import java.util.TimeZone;

@Value
public class LogisticCenterConfiguration {

    private TimeZone timeZone;

    public ZoneId getZoneId() {
        return timeZone.toZoneId();
    }
}
