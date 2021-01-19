package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;

@Builder
@Data
public class EntityRow {

    private ZonedDateTime date;

    private RowName rowName;

    private String value;

    private Source source;

    public LocalTime getTime() {
        return date.toLocalTime();
    }

    public EntityRow convertTimeZone(final ZoneId zoneId) {
        date = date.withZoneSameInstant(zoneId);
        return this;
    }

    public static EntityRow fromEntity(final Entity response) {
        RowName rowName = null;

        switch (response.getProcessName()) {
            case PACKING:
                rowName = RowName.PACKING;
                break;
            case PACKING_WALL:
                rowName = RowName.PACKING_WALL;
                break;
            case PICKING:
                rowName = RowName.PICKING;
                break;
            default:
                break;
        }
        return EntityRow.builder()
                .date(response.getDate())
                .rowName(rowName)
                .value(String.valueOf(response.getValue()))
                .source(response.getSource())
                .build();
    }
}
