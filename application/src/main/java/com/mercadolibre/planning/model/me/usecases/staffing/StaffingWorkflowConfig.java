package com.mercadolibre.planning.model.me.usecases.staffing;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum StaffingWorkflowConfig {
  FBM_WMS_OUTBOUND(
      List.of("picking", "batch_sorter", "wall_in", "packing", "packing_wall"), true, true),
  FBM_WMS_INBOUND(List.of("receiving", "check_in", "put_away"), true, true),
  FBM_WMS_WITHDRAWALS(List.of("picking", "packing"), false, false),
  FBM_WMS_STOCK(List.of("cycle_count", "inbound_audit", "stock_audit"), false, false),
  FBM_WMS_TRANSFER(List.of("picking"), false, false);

  private final List<String> processes;

  private final boolean shouldRetrieveProductivity;

  private final boolean shouldRetrieveHeadcount;

  public String getName() {
    return this.toString().toLowerCase().replace('_', '-');
  }
}
