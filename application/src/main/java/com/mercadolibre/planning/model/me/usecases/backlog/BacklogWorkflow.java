package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.CHECK_IN;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WAVING;
import static java.util.List.of;

@Getter
@AllArgsConstructor
enum BacklogWorkflow {
    FBM_WMS_OUTBOUND(of(WAVING, PICKING, PACKING), 0, 24),
    FBM_WMS_INBOUND(of(CHECK_IN, PUT_AWAY), 168, 168);

    private List<ProcessName> processNames;

    private int slaFromOffsetInHours;

    private int slaToOffsetInHours;

    static BacklogWorkflow from(final Workflow globalWorkflow) {
        return BacklogWorkflow.valueOf(globalWorkflow.name());
    }

}
