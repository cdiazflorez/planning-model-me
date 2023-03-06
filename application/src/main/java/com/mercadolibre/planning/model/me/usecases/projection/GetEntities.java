package com.mercadolibre.planning.model.me.usecases.projection;

import static com.mercadolibre.planning.model.me.enums.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.me.enums.ProcessName.CHECK_IN;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityFilters.ABILITY_LEVEL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityFilters.PROCESSING_TYPE;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType.ACTIVE_WORKERS;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType.EFFECTIVE_WORKERS;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source.FORECAST;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source.SIMULATION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getHourAndDay;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.action;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.createColumnHeaders;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.capitalize;

import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.Content;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTable;
import com.mercadolibre.planning.model.me.entities.projection.complextable.Data;
import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRow;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Productivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.RowName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SearchTrajectoriesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.gateways.toogle.FeatureSwitches;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Named;
import lombok.AllArgsConstructor;

/**
 * Builds planning tool entities table view.
 */
@Named
@AllArgsConstructor
public class GetEntities implements UseCase<GetProjectionInputDto, ComplexTable> {

  private static final DateTimeFormatter COLUMN_HOUR_FORMAT = ofPattern("HH:00");

  private static final String NO_VALUE = "-";

  private static final String FIRST_TITLE = "title_1";

  private static final String FIRST_SUBTITLE = "subtitle_1";

  private static final int HOURS_TO_SHOW = 25;

  private static final int MAIN_ABILITY_LEVEL = 1;

  private static final int POLYVALENT_ABILITY_LEVEL = 2;

  private final PlanningModelGateway planningModelGateway;

  private final LogisticCenterGateway logisticCenterGateway;

  private final FeatureSwitches featureSwitches;

  /**
   * Builds planning tool entities table view
   *
   * <p>
   * Retrieves headcount, productivity and tph for the follow 24 hours and builds a ComplexTable
   * to display this data.
   * </p>
   */
  @Override
  public ComplexTable execute(final GetProjectionInputDto input) {

    final LogisticCenterConfiguration config = logisticCenterGateway.getConfiguration(input.getWarehouseId());

    final ZonedDateTime utcDateFrom = input.getDate() == null
        ? getCurrentUtcDate() : input.getDate();
    final ZonedDateTime utcDateTo = utcDateFrom.plusDays(1);

    final Map<MagnitudeType, List<MagnitudePhoto>> entities = planningModelGateway
        .searchTrajectories(
            SearchTrajectoriesRequest.builder()
                .warehouseId(input.getWarehouseId())
                .workflow(input.getWorkflow())
                .entityTypes(List.of(HEADCOUNT, THROUGHPUT, PRODUCTIVITY))
                .dateFrom(utcDateFrom)
                .dateTo(utcDateTo)
                .processName(getProcessesByWorkflow(input.getWorkflow(), input.getWarehouseId()))
                .simulations(input.getSimulations())
                .entityFilters(Map.of(
                    HEADCOUNT, Map.of(
                        PROCESSING_TYPE.toJson(),
                        List.of(ACTIVE_WORKERS.getName(), EFFECTIVE_WORKERS.getName())
                    ),
                    PRODUCTIVITY, Map.of(
                        ABILITY_LEVEL.toJson(),
                        List.of(
                            valueOf(MAIN_ABILITY_LEVEL),
                            valueOf(POLYVALENT_ABILITY_LEVEL)
                        )
                    )
                ))
                .build()
        );

    final List<EntityRow> headcount = entities.get(HEADCOUNT).stream()
        .map(EntityRow::fromEntity)
        .collect(toList());

    final List<Productivity> productivities = entities.get(PRODUCTIVITY).stream()
        .filter(Productivity.class::isInstance)
        .map(Productivity.class::cast)
        .collect(toList());

    final List<EntityRow> mainProductivities = productivities.stream()
        .filter(productivity -> productivity.getAbilityLevel() == MAIN_ABILITY_LEVEL
            || productivity.getAbilityLevel() == 0)
        .map(EntityRow::fromEntity)
        .collect(toList());

    final List<EntityRow> polyvalentProductivities = productivities.stream()
        .filter(productivity -> productivity.getAbilityLevel() == POLYVALENT_ABILITY_LEVEL)
        .map(EntityRow::fromEntity)
        .collect(toList());

    List<EntityRow> throughputs = entities.get(THROUGHPUT)
        .stream()
        .map(EntityRow::fromEntity)
        .collect(toList());

    final List<ColumnHeader> headers = createColumnHeaders(
        convertToTimeZone(config.getZoneId(), utcDateFrom), HOURS_TO_SHOW);

    return new ComplexTable(
        headers,
        List.of(createData(config, HEADCOUNT, headcount, headers, emptyList()),
            createData(config, PRODUCTIVITY, mainProductivities, headers, polyvalentProductivities),
            createData(config, THROUGHPUT, throughputs, headers, emptyList())),
        action,
        "Headcount / Throughput"
    );
  }

  private Data createData(final LogisticCenterConfiguration config,
                          final MagnitudeType magnitudeType,
                          final List<EntityRow> entities,
                          final List<ColumnHeader> headers,
                          final List<EntityRow> polyvalentProductivity) {

    final Map<RowName, List<EntityRow>> entitiesByProcess = entities.stream()
        .collect(groupingBy(EntityRow::getRowName));
    final Map<RowName, List<EntityRow>> polyvalentPByProcess = polyvalentProductivity.stream()
        .collect(groupingBy((EntityRow::getRowName)));

    final boolean shouldOpenTab = magnitudeType == HEADCOUNT;

    return new Data(
        magnitudeType.getName(),
        capitalize(magnitudeType.getTitle()),
        shouldOpenTab,
        entitiesByProcess.entrySet().stream()
            .sorted(Comparator.comparing(entry -> entry.getKey().getIndex()))
            .map(entry -> createContent(
                config,
                magnitudeType,
                entry.getKey(),
                headers,
                entry.getValue(),
                polyvalentPByProcess.getOrDefault(entry.getKey(), emptyList())))
            .collect(toList())
    );
  }

  private Map<String, Content> createContent(final LogisticCenterConfiguration config,
                                             final MagnitudeType magnitudeType,
                                             final RowName processName,
                                             final List<ColumnHeader> headers,
                                             final List<EntityRow> entities,
                                             final List<EntityRow> polyvalentProductivity) {

    final Map<String, Map<Source, EntityRow>> entitiesByHour = entities.stream()
        .map(entity -> entity.convertTimeZone(config.getZoneId()))
        .collect(groupingBy(
            entity -> getHourAndDay(entity.getDate()),
            toMap(EntityRow::getSource, identity(), (e1, e2) -> e2)));

    final Map<String, String> polyvalentProductivityByHour = polyvalentProductivity.stream()
        .map(entity -> entity.convertTimeZone(config.getZoneId()))
        .collect(toMap(
            entity -> getHourAndDay(entity.getDate()),
            EntityRow::getValue,
            (value1, value2) -> value2));

    final Map<String, Content> content = new LinkedHashMap<>();

    headers.forEach(header -> {
      if ("column_1".equals(header.getId())) {
        content.put(header.getId(),
            new Content(capitalize(processName.getTitle()), null, null,
                processName.getName(), true));
      } else {
        final Map<Source, EntityRow> entityBySource = entitiesByHour.get(header.getValue());

        if (entityBySource != null && entityBySource.containsKey(SIMULATION)) {
          content.put(header.getId(), new Content(
              valueOf(entityBySource.get(SIMULATION).getValue()),
              entityBySource.get(SIMULATION).getDate(),
              createTooltip(
                  magnitudeType,
                  entityBySource.get(FORECAST),
                  polyvalentProductivityByHour.getOrDefault(
                      header.getValue(), NO_VALUE)
              ), null, true));
        } else if (entityBySource != null && entityBySource.containsKey(FORECAST)) {
          content.put(header.getId(), new Content(
              valueOf(entityBySource.get(FORECAST).getValue()),
              entityBySource.get(FORECAST).getDate(),
              null, null, true));
        } else {
          content.put(header.getId(),
              new Content(NO_VALUE, null, null, null, true));
        }
      }
    });
    return content;
  }

  private Map<String, String> createTooltip(final MagnitudeType magnitudeType,
                                            final EntityRow entity,
                                            final String polyvalentProductivity) {
    switch (magnitudeType) {
      case HEADCOUNT:
        return Map.of(
            FIRST_TITLE, "Hora de operación",
            FIRST_SUBTITLE, format("%s - %s",
                entity.getTime().format(COLUMN_HOUR_FORMAT),
                entity.getTime().plusHours(1).format(COLUMN_HOUR_FORMAT)),
            "title_2", "Cantidad de reps FCST",
            "subtitle_2", valueOf(entity.getValue())
        );
      case PRODUCTIVITY:
        return Map.of(
            FIRST_TITLE, "Productividad polivalente",
            FIRST_SUBTITLE, format("%s uds/h", polyvalentProductivity));
      default:
        return null;
    }
  }

  // TODO Remove this method when the transition to project with the library is finished
  private List<ProcessName> getProcessesByWorkflow(final Workflow workflow,
                                                   final String logisticCenterId) {

    if (workflow == FBM_WMS_OUTBOUND && featureSwitches.isProjectionLibEnabled(logisticCenterId)) {
      return List.of(PICKING, PACKING, BATCH_SORTER, WALL_IN, PACKING_WALL);
    }
    if (workflow == FBM_WMS_OUTBOUND && !featureSwitches.isProjectionLibEnabled(logisticCenterId)) {
      return List.of(PICKING, PACKING, PACKING_WALL);
    }
    if (workflow == FBM_WMS_INBOUND) {
      return List.of(PUT_AWAY, CHECK_IN);
    }

    return List.of();
  }
}
