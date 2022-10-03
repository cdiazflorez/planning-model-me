package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BacklogWorkflow {
    FBM_WMS_OUTBOUND(0, 24),
    FBM_WMS_INBOUND(168, 168);

  private final int slaFromOffsetInHours;

  private final int slaToOffsetInHours;

  public static BacklogWorkflow from(final Workflow globalWorkflow) {
    return BacklogWorkflow.valueOf(globalWorkflow.name());
  }
}
