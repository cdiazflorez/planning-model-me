package com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process;

import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.IMMEDIATE_BACKLOG;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.TOTAL_BACKLOG;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ProcessOutbound {

  OUTBOUND_PLANNING(
      "pending",
      "Ready to Wave",
      "Waving",
      0,
      asList(TOTAL_BACKLOG, IMMEDIATE_BACKLOG)),
  PICKING(
      "to_pick,to_route",
      "Ready to Pick, Picking",
      "Picking",
      1,
      singletonList(TOTAL_BACKLOG)),
  BATCH_SORTED(
      "to_sort",
      "Ready to Sort",
      "Batch Sorter",
      2,
      singletonList(TOTAL_BACKLOG)),
  WALL_IN(
      "sorted,to_group,grouping",
      "Ready to Group, Sorted, Grouping",
      "Wall",
      3,
      singletonList(TOTAL_BACKLOG)),
  PACKING(
      getToPack(),
      getReadyToPack(),
      "Packing Regular",
      4,
      singletonList(TOTAL_BACKLOG)),
  PACKING_WALL(
      getToPack(),
      getReadyToPack(),
      "Packing Wall",
      5,
      singletonList(TOTAL_BACKLOG));

  private final String status;
  private final String subtitle;
  private final String title;
  private final Integer index;
  private final List<MetricType> metricTypes;

  public static ProcessOutbound fromTitle(String title) {
    return stream(ProcessOutbound.values())
        .filter(pInfo -> pInfo.getTitle().equalsIgnoreCase(title))
        .findFirst()
        .orElse(null);
  }

  private static String getToPack() {
    return "to_pack";
  }

  private static String getReadyToPack() {
    return "Ready to Pack";
  }
}
