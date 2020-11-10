package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Unit {

    private long id;
    private String inventoryId;
    private UnitStatus status;
    private UnitGroup group;

    public UnitGroupCardinality getGroupCardinality() {
        return group.getQuantity() == 1
                ? UnitGroupCardinality.MONO
                : UnitGroupCardinality.MULTI;
    }
}
