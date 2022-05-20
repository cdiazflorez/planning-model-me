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
import com.mercadolibre.planning.model.me.usecases.backlog.entities.NumberOfUnitsInAnArea.NumberOfUnitsInASubarea;
import com.mercadolibre.planning.model.me.usecases.projection.entities.HeadcountAtArea;
import com.mercadolibre.planning.model.me.usecases.projection.entities.HeadcountBySubArea;
import com.mercadolibre.planning.model.me.usecases.projection.entities.RepsByArea;
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

  private static final String PRODUCTIVITY = "effective_productivity";

  private static final String SEPARATOR = "-";

  private final StaffingGateway staffingGateway;

  private final PlanningModelGateway planningModelGateway;


  public Map<Instant, List<HeadcountAtArea>> getProjectionHeadcount(final String warehouseId,
                                                                    final Map<Instant, List<NumberOfUnitsInAnArea>> backlogs) {


    final Instant dateFrom = backlogs.keySet().stream().min(Comparator.naturalOrder()).orElse(Instant.now());
    final Instant dateTo = backlogs.keySet().stream().max(Comparator.naturalOrder()).orElse(Instant.now());

    final MetricResponse lastHourEffectiveProductivity = staffingGateway.getMetricsByName(
        warehouseId,
        PRODUCTIVITY,
        new MetricRequest(ProcessName.PICKING, dateFrom.minus(1, ChronoUnit.HOURS), dateFrom));

    final Map<String, Double> mapLastHourEffectiveProductivity = lastHourEffectiveProductivity.getProcesses()
        .get(0)
        .getAreas()
        .stream()
        .collect(Collectors.toMap(AreaResponse::getName, AreaResponse::getValue));

    final List<MagnitudePhoto> operatingHoursPlannedHeadcount = planningModelGateway.getTrajectories(TrajectoriesRequest.builder()
        .warehouseId(warehouseId)
        .dateFrom(ZonedDateTime.ofInstant(dateFrom, ZoneOffset.UTC))
        .dateTo(ZonedDateTime.ofInstant(dateTo, ZoneOffset.UTC))
        .source(Source.SIMULATION)
        .processName(List.of(ProcessName.PICKING))
        .processingType(List.of(ProcessingType.ACTIVE_WORKERS))
        .workflow(Workflow.FBM_WMS_OUTBOUND)
        .entityType(MagnitudeType.HEADCOUNT)
        .build());

    return backlogs.entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, backlogArea -> {

          final Optional<MagnitudePhoto> headcountOptional = operatingHoursPlannedHeadcount.stream()
              .filter(magnitudePhoto -> magnitudePhoto.getDate().toInstant().equals(backlogArea.getKey()))
              .findAny();

          if (headcountOptional.isPresent()) {
            final int plannedHeadcount = headcountOptional.get().getValue();

            final List<HeadcountBySubArea> headcountProjectionList = getHeadcountBySubArea(
                backlogArea.getValue(),
                mapLastHourEffectiveProductivity,
                plannedHeadcount
            );

            redistributeHeadcount(headcountProjectionList, plannedHeadcount);

            return headcountProjectionList.stream().collect(Collectors.groupingBy(this::getArea))
                .entrySet()
                .stream()
                .map(value -> {
                  Integer reps = value.getValue()
                      .stream()
                      .mapToInt(HeadcountBySubArea::getReps)
                      .sum();
                  Double repsPercentage = value.getValue()
                      .stream()
                      .mapToDouble(HeadcountBySubArea::getRespPercentage)
                      .sum();
                  return new HeadcountAtArea(value.getKey(), reps, repsPercentage, value.getValue());

                })
                .collect(Collectors.toList());

          } else {
            return Collections.emptyList();
          }
        }));

  }

  private List<HeadcountBySubArea> getHeadcountBySubArea(final List<NumberOfUnitsInAnArea> backlogs,
                                                         final Map<String, Double> mapLastHourEffectiveProductivity,
                                                         final int plannedHeadcount) {
    final Map<String, Double> calculatedHC = backlogs.stream()
        .flatMap(v -> v.getSubareas().stream())
        .collect(Collectors.toMap(
            NumberOfUnitsInASubarea::getName,
            subAreas -> calculateRequiredHeadcountForArea(subAreas, mapLastHourEffectiveProductivity)));

    final double totalRequiredHeadcount = calculatedHC.values().stream().reduce(0D, Double::sum);
    final double difHC = plannedHeadcount - totalRequiredHeadcount;

    final Map<String, Double> headcountRemainderAssignByArea = calculatedHC.entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, value -> (value.getValue() / totalRequiredHeadcount) * difHC));

    final List<RepsByArea> repsByAreas = calculatedHC.entrySet()
        .stream()
        .map(value -> new RepsByArea(value.getKey(), value.getValue() + headcountRemainderAssignByArea.get(value.getKey())))
        .collect(Collectors.toList());

    return repsByAreas.stream()
        .sorted(Comparator.comparing(value -> value.getReps() % 1, Comparator.reverseOrder()))
        .map(repsByArea -> new HeadcountBySubArea(
            repsByArea.getArea(),
            repsByArea.getReps().intValue(),
            repsByArea.getReps() / plannedHeadcount))
        .collect(Collectors.toList());
  }

  private void redistributeHeadcount(final List<HeadcountBySubArea> headcountProjectionList, final int plannedHeadcount) {

    final int projectedHC = headcountProjectionList.stream()
        .mapToInt(HeadcountBySubArea::getReps)
        .sum();

    if (plannedHeadcount > projectedHC && !headcountProjectionList.isEmpty()) {
      final int difHeadcount = plannedHeadcount - projectedHC;
      for (int i = 0; i < difHeadcount; i++) {
        HeadcountBySubArea reps = headcountProjectionList.get(i % headcountProjectionList.size());
        reps.setReps(reps.getReps() + 1);
      }
    }
  }

  private String getArea(HeadcountBySubArea headcountBySubArea) {
    return headcountBySubArea.getSubArea().split(SEPARATOR)[0];
  }

  private double calculateRequiredHeadcountForArea(final NumberOfUnitsInASubarea backlog, final Map<String, Double> effectiveProductivity) {

    double productivityByArea = effectiveProductivity.get(backlog.getName()) == null
        ? effectiveProductivity.getOrDefault(backlog.getName().split(SEPARATOR)[0], 0D)
        : effectiveProductivity.get(backlog.getName());

    return productivityByArea == 0D ? 0D : backlog.getUnits() / productivityByArea;
  }


}
