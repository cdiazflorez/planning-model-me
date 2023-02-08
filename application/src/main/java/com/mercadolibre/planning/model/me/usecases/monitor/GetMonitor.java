package com.mercadolibre.planning.model.me.usecases.monitor;

import static com.mercadolibre.planning.model.me.utils.DateUtils.HOUR_MINUTES_FORMATTER;

import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.monitor.currentstatus.get.GetCurrentStatus;
import com.mercadolibre.planning.model.me.usecases.monitor.currentstatus.get.GetCurrentStatusInput;
import com.mercadolibre.planning.model.me.usecases.monitor.deviation.GetDeviation;
import com.mercadolibre.planning.model.me.usecases.monitor.deviation.GetDeviationInput;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.GetMonitorInput;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.Monitor;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.CurrentStatusData;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.DeviationData;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.MonitorData;
import java.time.ZonedDateTime;
import java.util.List;
import javax.inject.Named;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Named
@AllArgsConstructor
public class GetMonitor implements UseCase<GetMonitorInput, Monitor> {

  private final LogisticCenterGateway logisticCenterGateway;
  private final GetCurrentStatus getCurrentStatus;
  private final GetDeviation getDeviation;

  @Override
  public Monitor execute(GetMonitorInput input) {
    final ZonedDateTime currentTime = getCurrentTime(input.getWarehouseId());

    return Monitor.builder()
        .title("Modelo de Priorización")
        .subtitle1("Estado Actual")
        .subtitle2("Última actualización: Hoy - " + getPrettyTime(currentTime))
        .monitorData(getMonitorData(input, currentTime))
        .build();
  }

  private List<MonitorData> getMonitorData(GetMonitorInput input, ZonedDateTime currentTime) {
    final DeviationData deviationData = getDeviation.execute(
        GetDeviationInput.builder()
            .warehouseId(input.getWarehouseId())
            .workflow(input.getWorkflow())
            .dateFrom(input.getDateFrom())
            .dateTo(input.getDateTo())
            .currentTime(currentTime)
            .build());

    final CurrentStatusData currentStatusData = getCurrentStatus.execute(
        GetCurrentStatusInput.builder()
            .warehouseId(input.getWarehouseId())
            .workflow(input.getWorkflow())
            .dateFrom(input.getDateFrom())
            .dateTo(input.getDateTo())
            .groupType("order")
            .currentTime(currentTime)
            .build());

    return List.of(deviationData, currentStatusData);
  }

  private String getPrettyTime(ZonedDateTime date) {
    return date.format(HOUR_MINUTES_FORMATTER);
  }

  private ZonedDateTime getCurrentTime(String warehouseId) {
    final LogisticCenterConfiguration config =
        logisticCenterGateway.getConfiguration(warehouseId);

    return ZonedDateTime.now().withZoneSameInstant(config.getZoneId());
  }
}
