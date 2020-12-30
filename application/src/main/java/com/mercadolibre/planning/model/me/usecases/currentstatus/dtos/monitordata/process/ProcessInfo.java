package com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process;

import lombok.AllArgsConstructor;
import lombok.Getter;

import static java.util.Arrays.stream;

@AllArgsConstructor
@Getter
public enum ProcessInfo {
    OUTBOUND_PLANNING("pending", "Ready to Wave", "Outbound Planning", 0),
    PICKING("to_pick", "Ready to Pick", "Picking", 1),
    PACKING("to_pack", "Ready to Pack", "Packing", 2),
    WALL_IN("to_sort,sorted,to_group", "Ready to Group", "Wall In", 3);
    private final String status;
    private final String subtitle;
    private final String title;
    private final Integer index;
    
    public static ProcessInfo fromTitle(String title) {
        return stream(ProcessInfo.values())
                .filter(pInfo -> pInfo.getTitle().equalsIgnoreCase(title))
                .findFirst()
                .orElse(null);
    }
}
