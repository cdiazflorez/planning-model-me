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
import com.mercadolibre.planning.model.me.usecases.projection.entities.HeadCountByArea;
import com.mercadolibre.planning.model.me.usecases.projection.entities.HeadcountBySubArea;
import com.mercadolibre.planning.model.me.usecases.projection.entities.RepsByArea;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
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

  private final Double NUMBER_TO_ROUND = 100.0;

  public Map<Instant, List<HeadCountByArea>> getProjectionHeadcount(String warehouseId, Map<Instant, List<NumberOfUnitsInAnArea>> backlogs) {


    final Instant dateFrom = backlogs.keySet().stream().min(Comparator.comparing(Instant::toEpochMilli)).orElse(Instant.now());
    final Instant dateTo = backlogs.keySet().stream().max(Comparator.comparing(Instant::toEpochMilli)).orElse(Instant.now());

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

    return backlogs
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, we -> {

              Optional<MagnitudePhoto> headcountOptional =
                  headcountList.stream().filter(magnitudePhoto -> magnitudePhoto.getDate().toInstant().equals(we.getKey())).findAny();
              if (headcountOptional.isPresent()) {
                int headcount = headcountOptional.get().getValue();
                Map<String, Double> calculatedHC = we.getValue()
                    .stream()
                    .flatMap(v -> v.getSubareas().stream())
                    .collect(Collectors.toMap(NumberOfUnitsInAnArea.NumberOfUnitsInASubarea::getName, backlog -> hcArea(backlog, effectiveProductivity)));

                double totalHC = calculatedHC.values().stream().reduce(0D, Double::sum);
                double difHC = headcount - totalHC;

                Map<String, Double> remainderHC = calculatedHC.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, value -> (value.getValue() / totalHC )* difHC));

                List<RepsByArea> repsByAreas =  calculatedHC.entrySet()
                    .stream()
                    .map(value -> new RepsByArea(value.getKey(), value.getValue() + remainderHC.get(value.getKey()))).collect(Collectors.toList());


                List <HeadcountBySubArea> headcountProjectionList = repsByAreas.stream()
                    .sorted(Comparator.comparing(value -> value.getReps()%1, Comparator.reverseOrder()))
                    .map(repsByArea -> new HeadcountBySubArea(repsByArea.getArea(),
                        repsByArea.getReps().intValue(),
                        Math.round((repsByArea.getReps()/ headcount)*NUMBER_TO_ROUND)/NUMBER_TO_ROUND))
                    .collect(Collectors.toList());


                Integer projectedHC = headcountProjectionList.stream().reduce(0, (partialRepsResult, headcountBySubArea) -> partialRepsResult + headcountBySubArea.getReps(), Integer::sum);

                if(headcount > projectedHC && !headcountProjectionList.isEmpty()){
                  int difHeadcount = headcount - projectedHC;
                  while(difHeadcount > 0){
                    for(HeadcountBySubArea headcountBySubArea:headcountProjectionList){
                      if(difHeadcount == 0){
                        break;
                      }
                      headcountBySubArea.setReps(headcountBySubArea.getReps() +1);
                      difHeadcount--;
                    }
                  }
                }

                return headcountProjectionList.stream().collect(Collectors.groupingBy(this::getArea))
                    .entrySet()
                    .stream()
                    .map(value -> {
                      Integer reps = value.getValue()
                          .stream()
                          .reduce(0,(partialRepsResult, headcountBySubArea) -> partialRepsResult + headcountBySubArea.getReps(), Integer::sum);

                      Double repsPercentage = value.getValue()
                          .stream()
                          .mapToDouble(HeadcountBySubArea::getRespPercentage)
                          .sum();
                      return new HeadCountByArea(value.getKey(), reps,repsPercentage, value.getValue());

                    })
                    .collect(Collectors.toList());

              } else {
                return Collections.emptyList();
              }
            }));
    
  }

  private String getArea (HeadcountBySubArea headcountBySubArea){
    return headcountBySubArea.getSubArea().split("-")[0];
  }

  private double hcArea(final NumberOfUnitsInAnArea.NumberOfUnitsInASubarea backlog, final MetricResponse effectiveProductivity) {

    Optional<AreaResponse> productivityOptional = effectiveProductivity
        .getProcesses()
        .get(0)
        .getAreas()
        .stream()
        .filter(productivity -> productivity.getName().equals(backlog.getName()))
        .findAny();

    if (productivityOptional.isEmpty()) {

      productivityOptional = effectiveProductivity
          .getProcesses()
          .get(0)
          .getAreas()
          .stream()
          .filter(productivity -> productivity.getName().equals(backlog.getName().split("-")[0]))
          .findAny();
    }

    double productivity = productivityOptional.map(AreaResponse::getValue).orElse(0D);

    return productivity == 0D ? 0D : backlog.getUnits() / productivity;
  }


}
