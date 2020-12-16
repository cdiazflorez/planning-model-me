package com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ProcessInfo {
    OUTBOUND_PLANNING("pending", "Ready to Wave", "Outbound Planning"),
    PICKING("to_pick", "Ready to Pick", "Picking"),
    PACKING("to_pack", "Ready to Pack", "Packing"),
    WALL_IN("to_sort,sorted,to_group", "Ready to Group", "Wall In");
    private final String status;
    private final String subtitle;
    private final String title;
}
