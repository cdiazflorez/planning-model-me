package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound;

import com.mercadolibre.planning.model.me.enums.ProcessPath;
import com.mercadolibre.planning.model.me.exception.ForecastParsingException;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.OutboundForecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistribution;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import java.beans.JavaBean;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

@JavaBean
@AllArgsConstructor
public class ProcessPathValidator implements Consumer<Forecast> {

  private static final String STAFFING_SHEET = "PP - Staffing";

  private final Set<ProcessPath> processPathsAvailable;

  private final Workflow workflow;

  @Override
  public void accept(final Forecast forecast) {
    final var outboundForecast = (OutboundForecast) forecast;

    if (!workflow.equals(Workflow.FBM_WMS_OUTBOUND)
        || outboundForecast.getSheets().stream().noneMatch(sheet -> STAFFING_SHEET.equals(sheet.name()))) {
      return;
    }

    final var sheetProcessPaths = outboundForecast.getPlanningDistributions().stream()
        .map(PlanningDistribution::getProcessPath)
        .collect(Collectors.toSet());

    sheetProcessPaths.forEach(processPath -> {
          if (!processPath.equals(ProcessPath.GLOBAL) && !processPathsAvailable.contains(processPath)) {
            final String pp = processPathsAvailable.stream().map(ProcessPath::getName).collect(Collectors.joining(", "));

            throw new ForecastParsingException(
                String.format(" %s is an invalid process path for this warehouse. PP available : %s", processPath.getName(), pp)
            );
          }
        }
    );
  }
}
