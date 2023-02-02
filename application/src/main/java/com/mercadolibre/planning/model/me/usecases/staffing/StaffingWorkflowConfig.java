package com.mercadolibre.planning.model.me.usecases.staffing;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum StaffingWorkflowConfig {
  FBM_WMS_OUTBOUND(
      List.of(
          ProcessName.PICKING.getName(),
          ProcessName.BATCH_SORTER.getName(),
          ProcessName.WALL_IN.getName(),
          ProcessName.PACKING.getName(),
          ProcessName.PACKING_WALL.getName(),
          ProcessName.HU_ASSEMBLY.getName(),
          ProcessName.SALES_DISPATCH.getName()),
      true,
      true),
  FBM_WMS_INBOUND(
      List.of(
          ProcessName.RECEIVING.getName(),
          ProcessName.CHECK_IN.getName(),
          ProcessName.PUT_AWAY.getName()),
      true,
      true),
  FBM_WMS_WITHDRAWALS(
      List.of(ProcessName.PICKING.getName(), ProcessName.PACKING.getName()), false, false),
  FBM_WMS_STOCK(List.of("cycle_count", "inbound_audit", "stock_audit"), false, false),
  FBM_WMS_TRANSFER(List.of(ProcessName.PICKING.getName()), false, false);

  private final List<String> processes;

  private final boolean shouldRetrieveProductivity;

  private final boolean shouldRetrieveHeadcount;

  public String getName() {
    return this.toString().toLowerCase().replace('_', '-');
  }
}
