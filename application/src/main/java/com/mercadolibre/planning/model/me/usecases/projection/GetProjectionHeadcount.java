package com.mercadolibre.planning.model.me.usecases.projection;

import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.TrajectoriesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.gateways.staffing.StaffingGateway;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.request.MetricRequest;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.AreaResponse;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.MetricResponse;
import com.mercadolibre.planning.model.me.usecases.backlog.entities.NumberOfUnitsInAnArea;
import com.mercadolibre.planning.model.me.usecases.projection.entities.SuggestedHeadcount;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Named;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Named
@AllArgsConstructor
@Slf4j
public class GetProjectionHeadcount {

  private final StaffingGateway staffingGateway;

  private final PlanningModelGateway planningModelGateway;

  public void getProjectionHeadcount(String warehouseId, Instant dateFrom, Instant dateTo,
                                     Map<Instant, List<NumberOfUnitsInAnArea>> backlogs) {


    MetricResponse effectiveProductivity =
        staffingGateway.getMetricsByName(warehouseId, "effective_productivity", new MetricRequest(ProcessName.PICKING,
            dateFrom.minus(1, ChronoUnit.HOURS),
            dateFrom));

    List<MagnitudePhoto> headcountList = planningModelGateway.getTrajectories(TrajectoriesRequest.builder()
        .warehouseId(warehouseId)
        .dateFrom(ZonedDateTime.ofInstant(dateFrom, ZoneOffset.UTC))
        .dateTo(ZonedDateTime.ofInstant(dateTo, ZoneOffset.UTC))
        .source(Source.SIMULATION)
        .processName(List.of(ProcessName.PICKING))
        .processingType(List.of(ProcessingType.ACTIVE_WORKERS))
        .workflow(Workflow.FBM_WMS_OUTBOUND)
        .entityType(MagnitudeType.HEADCOUNT)
        .build());

    Map<Instant, List<SuggestedHeadcount>> suggestedHeadCount =
        backlogs
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, we -> {

              Optional<MagnitudePhoto> headcountOptional =
                  headcountList.stream().filter(magnitudePhoto -> magnitudePhoto.getDate().toInstant().equals(we.getKey())).findAny();
              if (headcountOptional.isPresent()) {

                int headcount = headcountOptional.get().getValue();
                Map<String, Double> calculatedHC = we.getValue()
                    .stream()
                    .collect(Collectors.toMap(NumberOfUnitsInAnArea::getArea, backlog -> hcArea(backlog, effectiveProductivity)));

                double totalHC = calculatedHC.values().stream().reduce(0D, Double::sum);
                double difHC = headcount - totalHC;

                Map<String, Double> remainderHC = calculatedHC.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, value -> (value.getValue() / totalHC )* difHC));

                return calculatedHC.entrySet()
                    .stream()
                    .map(value -> new SuggestedHeadcount(value.getKey(), value.getValue() + remainderHC.get(value.getKey())))
                    .collect(Collectors.toList());

              } else {
                return Collections.emptyList();
              }
            }));


  }

  private double hcArea(NumberOfUnitsInAnArea backlog, MetricResponse effectiveProductivity) {

    double reps = 0D;

    Optional<AreaResponse> productivityOptional = effectiveProductivity
        .getProcesses()
        .get(0)
        .getAreas()
        .stream()
        .filter(productivity -> productivity.getName().equals(backlog.getArea()))
        .findAny();

    if (productivityOptional.isPresent()) {

      AreaResponse productivity = productivityOptional.get();
      reps = backlog.getUnits() / productivity.getValue();
    }

    return reps;
  }


}
