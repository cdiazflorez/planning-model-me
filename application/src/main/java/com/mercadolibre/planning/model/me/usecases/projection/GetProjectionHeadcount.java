package com.mercadolibre.planning.model.me.usecases.projection;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType.EFFECTIVE_WORKERS;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.outboundsettings.SettingsGateway;
import com.mercadolibre.planning.model.me.gateways.outboundsettings.dtos.AreaConfiguration;
import com.mercadolibre.planning.model.me.gateways.outboundsettings.dtos.SettingsAtWarehouse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType;
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

  private static final String RK_AREA_ID = "RK";

  private static final int EFFECTIVE_PRODUCTIVITY_LOOKBACK_IN_DAYS = 20;

  private static final int MINIMUM_RKH_VALUE = 2;

  private final StaffingGateway staffingGateway;

  private final PlanningModelGateway planningModelGateway;

  private final SettingsGateway settingsGateway;

  public Map<Instant, List<HeadcountAtArea>> getProjectionHeadcount(final String warehouseId,
                                                                    final Map<Instant, List<NumberOfUnitsInAnArea>> backlogs) {


    final Instant dateFrom = backlogs.keySet().stream().min(Comparator.naturalOrder()).orElse(Instant.now());
    final Instant dateTo = backlogs.keySet().stream().max(Comparator.naturalOrder()).orElse(Instant.now());

    final SettingsAtWarehouse areaConfiguration = getSettings(warehouseId);

    final MetricResponse lastHourEffectiveProductivity = staffingGateway.getMetricsByName(
        warehouseId,
        PRODUCTIVITY,
        new MetricRequest(ProcessName.PICKING, dateFrom.minus(EFFECTIVE_PRODUCTIVITY_LOOKBACK_IN_DAYS, ChronoUnit.DAYS), dateFrom));

    final Map<String, Double> mapLastHourEffectiveProductivity = lastHourEffectiveProductivity.getProcesses()
        .get(0)
        .getAreas()
        .stream()
        .collect(Collectors.toMap(AreaResponse::getName, AreaResponse::getValue));

    final List<MagnitudePhoto> operatingHoursPlannedHeadcount = planningModelGateway
        .getTrajectories(TrajectoriesRequest.builder()
            .warehouseId(warehouseId)
            .dateFrom(ZonedDateTime.ofInstant(dateFrom, ZoneOffset.UTC))
            .dateTo(ZonedDateTime.ofInstant(dateTo, ZoneOffset.UTC))
            .source(Source.SIMULATION)
            .processName(List.of(ProcessName.PICKING))
            .processingType(List.of(EFFECTIVE_WORKERS))
            .workflow(Workflow.FBM_WMS_OUTBOUND)
            .entityType(MagnitudeType.HEADCOUNT)
            .build());

    return backlogs.entrySet().stream().collect(Collectors.toMap(
        Map.Entry::getKey,
        backlogArea -> getHeadcount(operatingHoursPlannedHeadcount, backlogArea.getKey())
            .map(headcount -> getHeadcountAtArea(backlogArea.getValue(), mapLastHourEffectiveProductivity, headcount, areaConfiguration))
            .orElse(Collections.emptyList())
    ));

  }

  private List<HeadcountAtArea> getHeadcountAtArea(final List<NumberOfUnitsInAnArea> backlogs,
                                                   final Map<String, Double> mapLastHourEffectiveProductivity,
                                                   final Integer plannedHeadcount,
                                                   final SettingsAtWarehouse areaConfiguration) {

    final List<HeadcountBySubArea> headcountProjectionList = getHeadcountBySubArea(
        backlogs,
        mapLastHourEffectiveProductivity,
        plannedHeadcount,
        areaConfiguration
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


  }

  private Optional<Integer> getHeadcount(final List<MagnitudePhoto> operatingHoursPlannedHeadcount, final Instant headcountKey) {

    Optional<MagnitudePhoto> magnitudePhoto =
        operatingHoursPlannedHeadcount.stream()
            .filter(entity -> entity.getDate().toInstant().equals(headcountKey) && entity.getSource().equals(Source.SIMULATION))
            .findAny()
            .or(() -> operatingHoursPlannedHeadcount.stream()
                .filter(entity -> entity.getDate().toInstant().equals(headcountKey) && entity.getSource().equals(Source.FORECAST))
                .findAny());

    return magnitudePhoto.map(MagnitudePhoto::getValue);
  }

  private List<HeadcountBySubArea> getHeadcountBySubArea(final List<NumberOfUnitsInAnArea> backlogs,
                                                         final Map<String, Double> mapLastHourEffectiveProductivity,
                                                         final int plannedHeadcount,
                                                         final SettingsAtWarehouse areaConfiguration) {

    final Map<String, Double> calculatedHC = backlogs.stream()
        .flatMap(v -> v.getSubareas().stream())
        .collect(Collectors.toMap(
            NumberOfUnitsInASubarea::getName,
            subAreas -> calculateRequiredHeadcountForArea(subAreas, mapLastHourEffectiveProductivity, areaConfiguration)));

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

  private double calculateRequiredHeadcountForArea(final NumberOfUnitsInASubarea backlog,
                                                   final Map<String, Double> effectiveProductivity,
                                                   final SettingsAtWarehouse areaConfiguration) {

    final String areaName = validateRKArea(backlog.getName(), areaConfiguration);
    double productivityByArea = effectiveProductivity.get(areaName) == null
        ? effectiveProductivity.getOrDefault(areaName.split(SEPARATOR)[0], 0D)
        : effectiveProductivity.get(areaName);

    return productivityByArea == 0D ? 0D : backlog.getUnits() / productivityByArea;
  }

  private String validateRKArea(String name, SettingsAtWarehouse areaConfiguration) {
    final String[] splitName = name.split(SEPARATOR);
    if (areaConfiguration == null) {
      return name;
    } else if (name.contains(RK_AREA_ID) && splitName.length > 1) {
      final String floor = splitName[1];
      final int forkliftLevel = areaConfiguration.getAreas().stream()
          .filter(area -> RK_AREA_ID.equals(area.getId()) && area.getFloor().equals(floor))
          .mapToInt(AreaConfiguration::getForkliftLevel)
          .findFirst()
          .orElse(0);

      return forkliftLevel >= MINIMUM_RKH_VALUE ? "RK-H" : "RK-L";
    } else {
      return name;
    }
  }

  private SettingsAtWarehouse getSettings(String warehouseId) {
    try {
      return settingsGateway.getPickingSetting(warehouseId);
    } catch (RuntimeException e) {
      return null;
    }

  }

}
