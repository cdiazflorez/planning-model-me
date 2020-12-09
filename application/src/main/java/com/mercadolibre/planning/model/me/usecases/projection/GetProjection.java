package com.mercadolibre.planning.model.me.usecases.projection;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.ComplexTable;
import com.mercadolibre.planning.model.me.entities.projection.Content;
import com.mercadolibre.planning.model.me.entities.projection.Data;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.ProjectionResult;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.entities.projection.chart.Chart;
import com.mercadolibre.planning.model.me.entities.projection.chart.ChartData;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ConfigurationRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ConfigurationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRow;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Productivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProductivityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.RowName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklog;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogInputDto;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.sales.GetSales;
import com.mercadolibre.planning.model.me.usecases.sales.dtos.GetSalesInputDto;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.RowName.DEVIATION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source.FORECAST;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source.SIMULATION;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getHourAndDay;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang.StringUtils.capitalize;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class GetProjection implements UseCase<GetProjectionInputDto, Projection> {

    private static final DateTimeFormatter COLUMN_HOUR_FORMAT = ofPattern("HH:00");
    private static final DateTimeFormatter CPT_HOUR_FORMAT = ofPattern("HH:mm");
    private static final int HOURS_TO_SHOW = 25;
    private static final int SELLING_PERIOD_HOURS = 28;
    private static final String PROCESSING_TIME = "processing_time";
    private static final List<ProcessingType> PROJECTION_PROCESSING_TYPES =
            List.of(ProcessingType.ACTIVE_WORKERS);
    private static final int MAIN_ABILITY_LEVEL = 1;
    private static final int POLYVALENT_ABILITY_LEVEL = 2;

    protected static final List<ProcessName> PROJECTION_PROCESS_NAMES = List.of(PICKING, PACKING);

    protected final PlanningModelGateway planningModelGateway;
    protected final LogisticCenterGateway logisticCenterGateway;
    protected final GetBacklog getBacklog;
    protected final GetSales getSales;

    @Override
    public Projection execute(final GetProjectionInputDto input) {
        final ZonedDateTime utcDateFrom = getCurrentUtcDate();
        final ZonedDateTime utcDateTo = utcDateFrom.plusDays(1);

        final List<EntityRow> headcount = planningModelGateway.getEntities(
                createRequest(input, HEADCOUNT, utcDateFrom, utcDateTo,
                        PROJECTION_PROCESSING_TYPES))
                .stream()
                .map(EntityRow::fromEntity)
                .collect(toList());

        final List<Productivity> productivities = planningModelGateway.getProductivity(
                createProductivityRequest(input, utcDateFrom, utcDateTo));

        final List<EntityRow> mainProductivities = productivities.stream()
                .filter(productivity -> productivity.getAbilityLevel() == MAIN_ABILITY_LEVEL
                        || productivity.getAbilityLevel() == 0)
                .map(EntityRow::fromEntity)
                .collect(toList());

        final List<EntityRow> polyvalentProductivities = productivities.stream()
                .filter(productivity -> productivity.getAbilityLevel() == POLYVALENT_ABILITY_LEVEL)
                .map(EntityRow::fromEntity)
                .collect(toList());

        List<EntityRow> throughputs = planningModelGateway.getEntities(
                createRequest(input, THROUGHPUT, utcDateFrom, utcDateTo, null))
                .stream()
                .map(EntityRow::fromEntity)
                .collect(toList());

        final List<Backlog> backlogs = getBacklog.execute(
                new GetBacklogInputDto(input.getWorkflow(), input.getWarehouseId())
        );

        final List<Backlog> sales = getSales.execute(new GetSalesInputDto(
                input.getWorkflow(),
                input.getWarehouseId(),
                utcDateFrom.minusHours(SELLING_PERIOD_HOURS))
        );

        final List<ProjectionResult> projections = getProjection(
                input, utcDateFrom, utcDateTo, backlogs);

        final LogisticCenterConfiguration config = logisticCenterGateway.getConfiguration(
                input.getWarehouseId());

        final List<PlanningDistributionResponse> planningDistribution = planningModelGateway
                .getPlanningDistribution(new PlanningDistributionRequest(
                        input.getWarehouseId(),
                        input.getWorkflow(),
                        utcDateFrom,
                        utcDateFrom,
                        utcDateTo));

        final ProcessingTime processingTime = createProcessingTimeObject(
                planningModelGateway.getConfiguration(
                        ConfigurationRequest.builder()
                                .warehouseId(input.getWarehouseId())
                                .key(PROCESSING_TIME)
                                .build()));

        final List<ColumnHeader> headers = createColumnHeaders(config, utcDateFrom);
        addDeviationEntities(throughputs, projections);

        return new Projection(
                "Proyecciones",
                new ComplexTable(
                        headers,
                        List.of(createData(config, HEADCOUNT, headcount, headers, emptyList()),
                                createData(config, PRODUCTIVITY, mainProductivities,
                                        headers, polyvalentProductivities),
                                createData(config, THROUGHPUT, throughputs, headers, emptyList()))
                ),
                createProjectionDetailsTable(
                        backlogs,
                        sales,
                        projections,
                        config,
                        planningDistribution,
                        processingTime),
                new Chart(
                        processingTime,
                        toChartData(projections, config.getZoneId(), utcDateTo)
                )
        );
    }

    private ProcessingTime createProcessingTimeObject(
            Optional<ConfigurationResponse> processingTimeConfiguration) {
        return processingTimeConfiguration
                .map(configurationResponse -> new ProcessingTime(configurationResponse.getValue(),
                configurationResponse.getMetricUnit().getName()))
                .orElseGet(() -> new ProcessingTime(60, MINUTES.getName()));
    }

    private SimpleTable createProjectionDetailsTable(
            final List<Backlog> backlogs,
            final List<Backlog> sales,
            final List<ProjectionResult> projectionResults,
            final LogisticCenterConfiguration configuration,
            final List<PlanningDistributionResponse> planningDistribution,
            final ProcessingTime processingTime) {

        final ZoneId zoneId = configuration.getTimeZone().toZoneId();
        final boolean hasSimulatedResults = hasSimulatedResults(projectionResults);

        return new SimpleTable(
                "Resumen de Proyección",
                getProjectionDetailsTableColumns(hasSimulatedResults),
                projectionResults.stream()
                        .sorted(Comparator.comparing(ProjectionResult::getDate).reversed())
                        .map(projection -> getProjectionDetailsTableData(
                                backlogs,
                                sales,
                                planningDistribution,
                                processingTime,
                                zoneId,
                                projection,
                                hasSimulatedResults)
                        )
                        .collect(toList())
        );
    }

    private boolean hasSimulatedResults(List<ProjectionResult> projectionResults) {
        return projectionResults.stream().anyMatch(p -> p.getSimulatedEndDate() != null);
    }

    private Map<String, String> getProjectionDetailsTableData(
            final List<Backlog> backlogs,
            final List<Backlog> sales,
            final List<PlanningDistributionResponse> planningDistribution,
            final ProcessingTime processingTime,
            final ZoneId zoneId,
            final ProjectionResult projection,
            final boolean hasSimulatedResults) {
        final ZonedDateTime cpt = projection.getDate();
        final ZonedDateTime projectedEndDate = projection.getProjectedEndDate();
        final ZonedDateTime simulatedEndDate = projection.getSimulatedEndDate();
        final int backlog = getBacklogQuantity(cpt, backlogs);
        final int soldItems = getBacklogQuantity(cpt, sales);

        final Map<String, String> data = new LinkedHashMap<>(Map.of(
                "style", getStyle(cpt, projectedEndDate, processingTime),
                "column_1", convertToTimeZone(zoneId, cpt).format(CPT_HOUR_FORMAT),
                "column_2", String.valueOf(backlog),
                "column_3", getDeviation(cpt, soldItems, planningDistribution),
                "column_4", projectedEndDate == null
                        ? "Excede las 24hs"
                        : convertToTimeZone(zoneId, projectedEndDate).format(CPT_HOUR_FORMAT)));

        if (hasSimulatedResults) {
            data.put(
                    "column_5",
                    simulatedEndDate == null
                            ? "Excede las 24hs"
                            : convertToTimeZone(zoneId, simulatedEndDate).format(CPT_HOUR_FORMAT)
            );
        }

        return data;
    }

    private List<ColumnHeader> getProjectionDetailsTableColumns(
            final boolean hasSimulatedResults) {

        final List<ColumnHeader> columnHeaders = new ArrayList<>(List.of(
                new ColumnHeader("column_1", "CPT's"),
                new ColumnHeader("column_2", "Backlog actual"),
                new ColumnHeader("column_3", "Desv. vs forecast")
        ));

        if (hasSimulatedResults) {
            columnHeaders.add(new ColumnHeader("column_4", "Cierre actual"));
            columnHeaders.add(new ColumnHeader("column_5", "Cierre simulado"));
        } else {
            columnHeaders.add(new ColumnHeader("column_4", "Cierre proyectado"));
        }

        return columnHeaders;
    }

    private String getStyle(final ZonedDateTime cpt,
                            final ZonedDateTime projectedEndDate,
                            final ProcessingTime processingTime) {
        if (projectedEndDate == null || projectedEndDate.isAfter(cpt)) {
            return "danger";
        } else if (projectedEndDate.isBefore(cpt.minusMinutes(processingTime.getValue()))) {
            return "none";
        } else {
            return "warning";
        }
    }

    private String getDeviation(final ZonedDateTime cpt,
                                final int backlogQuantity,
                                final List<PlanningDistributionResponse> planningDistribution) {
        final long forecastedItemsForCpt = planningDistribution
                .stream()
                .filter(distribution -> cpt.isEqual(distribution.getDateOut()))
                .mapToLong(PlanningDistributionResponse::getTotal)
                .sum();

        if (forecastedItemsForCpt == 0 || backlogQuantity == 0) {
            return "0%";
        }

        final double deviation = (((double) backlogQuantity / forecastedItemsForCpt) - 1) * 100;
        return String.format("%.1f%s", Math.round(deviation * 100.00) / 100.00, "%");
    }

    private int getBacklogQuantity(final ZonedDateTime cpt, final List<Backlog> backlogs) {
        final Optional<Backlog> cptBacklog = backlogs.stream()
                .filter(backlog -> cpt.isEqual(backlog.getDate()))
                .findFirst();

        return cptBacklog.map(Backlog::getQuantity).orElse(0);
    }

    private void addDeviationEntities(List<EntityRow> throughputs,
                                      List<ProjectionResult> projections) {

        final TreeMap<ZonedDateTime, Integer> quantityByDate = projections.stream()
                .collect(toMap(
                        o -> o.getDate().truncatedTo(HOURS),
                        ProjectionResult::getRemainingQuantity,
                        Integer::sum,
                        TreeMap::new));

        AtomicInteger acumulatedQuantity = new AtomicInteger(0);
        quantityByDate.forEach((dateTime, quantity) -> {
            if (quantity > 0) {
                acumulatedQuantity.addAndGet(quantity);
                throughputs.add(EntityRow.builder()
                        .date(dateTime)
                        .value(acumulatedQuantity + " (+" + quantity + ")")
                        .source(FORECAST)
                        .rowName(DEVIATION)
                        .build()
                );
            }
        });

        if (throughputs.stream().noneMatch(t -> t.getRowName().equals(DEVIATION))) {
            throughputs.add(EntityRow.builder()
                    .date(getCurrentUtcDate())
                    .value("-")
                    .source(FORECAST)
                    .rowName(DEVIATION)
                    .build());
        }
    }

    private EntityRequest createRequest(final GetProjectionInputDto input,
                                        final EntityType entityType,
                                        final ZonedDateTime dateFrom,
                                        final ZonedDateTime dateTo,
                                        final List<ProcessingType> processingType) {
        return EntityRequest.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .entityType(entityType)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .processName(PROJECTION_PROCESS_NAMES)
                .processingType(processingType)
                .simulations(input.getSimulations())
                .build();
    }

    private ProductivityRequest createProductivityRequest(
            final GetProjectionInputDto input,
            final ZonedDateTime dateFrom,
            final ZonedDateTime dateTo) {

        return ProductivityRequest.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .entityType(PRODUCTIVITY)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .processName(PROJECTION_PROCESS_NAMES)
                .simulations(input.getSimulations())
                .abilityLevel(List.of(MAIN_ABILITY_LEVEL, POLYVALENT_ABILITY_LEVEL))
                .build();
    }

    private List<ColumnHeader> createColumnHeaders(final LogisticCenterConfiguration config,
                                                   final ZonedDateTime utcDateFrom) {

        final ZonedDateTime dateFrom = convertToTimeZone(config.getZoneId(), utcDateFrom);
        final List<ColumnHeader> columns = new ArrayList<>(HOURS_TO_SHOW);

        columns.add(new ColumnHeader("column_1", "Horas de Operación"));
        columns.addAll(IntStream.range(0, HOURS_TO_SHOW)
                .mapToObj(index -> {
                    final ZonedDateTime date = dateFrom.plusHours(index);
                    return new ColumnHeader(
                            format("column_%s", 2 + index),
                            date.format(COLUMN_HOUR_FORMAT),
                            getHourAndDay(date));
                }).collect(toList()));

        return columns;
    }

    private Data createData(final LogisticCenterConfiguration config,
                            final EntityType entityType,
                            final List<EntityRow> entities,
                            final List<ColumnHeader> headers,
                            final List<EntityRow> polyvalentProductivity) {

        final Map<RowName, List<EntityRow>> entitiesByProcess = entities.stream()
                .collect(groupingBy(EntityRow::getRowName));
        final Map<RowName, List<EntityRow>> polyvalentPByProcess = polyvalentProductivity.stream()
                .collect(groupingBy((EntityRow::getRowName)));

        final boolean shouldOpenTab = entityType == HEADCOUNT;

        return new Data(
                entityType.getName(),
                capitalize(entityType.getTitle()),
                shouldOpenTab,
                entitiesByProcess.entrySet().stream()
                        .sorted(Comparator.comparing(entry -> entry.getKey().getIndex()))
                        .map(entry -> createContent(
                                config,
                                entityType,
                                entry.getKey(),
                                headers,
                                entry.getValue(),
                                polyvalentPByProcess.getOrDefault(entry.getKey(), emptyList())))
                        .collect(toList())
        );
    }

    private Map<String, Content> createContent(final LogisticCenterConfiguration config,
                                               final EntityType entityType,
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
                        new Content(capitalize(processName.getTitle()), null, null));
            } else {
                final Map<Source, EntityRow> entityBySource = entitiesByHour.get(header.getValue());

                if (entityBySource != null && entityBySource.containsKey(SIMULATION)) {
                    content.put(header.getId(), new Content(
                            valueOf(entityBySource.get(SIMULATION).getValue()),
                            entityBySource.get(SIMULATION).getDate(),
                            createTooltip(
                                    entityType,
                                    entityBySource.get(FORECAST),
                                    polyvalentProductivityByHour.getOrDefault(
                                            header.getValue(), "-")
                                    )));
                } else if (entityBySource != null && entityBySource.containsKey(FORECAST)) {
                    content.put(header.getId(), new Content(
                            valueOf(entityBySource.get(FORECAST).getValue()),
                            entityBySource.get(FORECAST).getDate(),
                            null));
                } else {
                    content.put(header.getId(), new Content("-", null, null));
                }
            }
        });
        return content;
    }

    private Map<String, String> createTooltip(final EntityType entityType,
                                              final EntityRow entity,
                                              final String polyvalentProductivity) {
        switch (entityType) {
            case HEADCOUNT:
                return Map.of(
                        "title_1", "Hora de operación",
                        "subtitle_1", format("%s - %s",
                                entity.getTime().format(COLUMN_HOUR_FORMAT),
                                entity.getTime().plusHours(1).format(COLUMN_HOUR_FORMAT)),
                        "title_2", "Cantidad de reps FCST",
                        "subtitle_2", valueOf(entity.getValue())
                );
            case PRODUCTIVITY:
                return Map.of(
                        "title_1", "Productividad polivalente",
                        "subtitle_1", format("%s uds/h", polyvalentProductivity));
            default:
                return null;
        }
    }

    private List<ChartData> toChartData(final List<ProjectionResult> projectionResult,
                                        final ZoneId zoneId,
                                        final ZonedDateTime dateTo) {
        final boolean hasSimulatedResults = hasSimulatedResults(projectionResult);

        return projectionResult.stream()
                .map(projection -> {
                    final ZonedDateTime projectedEndDate = hasSimulatedResults
                            ? projection.getSimulatedEndDate()
                            : projection.getProjectedEndDate();
                    return ChartData.fromProjection(
                            convertToTimeZone(zoneId, projection.getDate()),
                            convertToTimeZone(zoneId, projectedEndDate == null
                                    ? dateTo : projectedEndDate));
                })
                .collect(Collectors.toList());
    }

    protected abstract List<ProjectionResult> getProjection(final GetProjectionInputDto input,
                                                            final ZonedDateTime dateFrom,
                                                            final ZonedDateTime dateTo,
                                                            final List<Backlog> backlogs);

}
