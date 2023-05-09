package com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos;

import java.time.ZoneId;
import java.util.TimeZone;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class LogisticCenterConfiguration {

    TimeZone timeZone;

    boolean putToWall;

    public ZoneId getZoneId() {
        return timeZone.toZoneId();
    }

    public LogisticCenterConfiguration(TimeZone timeZone) {
        this.timeZone = timeZone;
        this.putToWall = false;
    }
}
