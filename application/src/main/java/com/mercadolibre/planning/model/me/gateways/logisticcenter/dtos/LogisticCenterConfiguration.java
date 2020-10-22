package com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos;

import lombok.Value;

import java.time.LocalDateTime;
import java.util.TimeZone;

@Value
public class LogisticCenterConfiguration {

    private TimeZone timeZone;

    public LocalDateTime getLocalDateTime() {
        return LocalDateTime.now(getTimeZone().toZoneId());
    }
}
