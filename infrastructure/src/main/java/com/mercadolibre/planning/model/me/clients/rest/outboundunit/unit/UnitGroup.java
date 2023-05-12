package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit;

import java.time.ZonedDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnitGroup {

    private long id;
    private String carrierName;
    private UnitGroupType type;
    private int quantity;
    private Carrier carrier;
    private ZonedDateTime estimatedTimeDeparture;
    private List<Unit> units;
}
