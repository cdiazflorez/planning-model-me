package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class EntityRow {

    private ZonedDateTime date;

    private RowName rowName;

    private String value;

    private Source source;

    private boolean valid;

    public LocalTime getTime() {
        return date.toLocalTime();
    }

    public EntityRow convertTimeZone(final ZoneId zoneId) {
        date = date.withZoneSameInstant(zoneId);
        return this;
    }

    public static EntityRow fromEntity(final MagnitudePhoto response, final boolean valid) {
        final RowName rowName = getName(response.getProcessName());

        return EntityRow.builder()
                .date(response.getDate())
                .valid(valid)
                .rowName(rowName)
                .value(String.valueOf(response.getValue()))
                .source(response.getSource())
                .build();
    }

    public static EntityRow fromEntity(final MagnitudePhoto response) {
        return fromEntity(response, true);
    }

    public static RowName getName(ProcessName processName) {
        RowName rowName = null;

        switch (processName) {
            case PACKING:
                rowName = RowName.PACKING;
                break;
            case BATCH_SORTER:
                rowName = RowName.BATCH_SORTER;
                break;
            case WALL_IN:
                rowName = RowName.WALL_IN;
                break;
            case PACKING_WALL:
                rowName = RowName.PACKING_WALL;
                break;
            case PICKING:
                rowName = RowName.PICKING;
                break;
            case PUT_AWAY:
                rowName = RowName.PUT_AWAY;
                break;
            case CHECK_IN:
                rowName = RowName.CHECK_IN;
                break;
            case GLOBAL:
                rowName = RowName.GLOBAL;
                break;
            default:
                break;
        }

        return rowName;
    }

}
