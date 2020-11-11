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
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklog;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogInputDto;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source.FORECAST;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source.SIMULATION;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getHourAndDay;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.time.format.DateTimeFormatter.ofPattern;
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
    private static final String PROCESSING_TIME = "processing_time";
    private static final List<ProcessingType> PROJECTION_PROCESSING_TYPES =
            List.of(ProcessingType.ACTIVE_WORKERS);

    protected static final List<ProcessName> PROJECTION_PROCESS_NAMES = List.of(PICKING, PACKING);

    protected final PlanningModelGateway planningModelGateway;
    protected final LogisticCenterGateway logisticCenterGateway;
    protected final GetBacklog getBacklog;

    @Override
    public Projection execute(final GetProjectionInputDto input) {
        final ZonedDateTime utcDateFrom = getCurrentUtcDate();
        final ZonedDateTime utcDateTo = utcDateFrom.plusDays(1);

        final List<Entity> headcount = planningModelGateway.getEntities(
                createRequest(input, HEADCOUNT, utcDateFrom, utcDateTo,
                        PROJECTION_PROCESSING_TYPES));

        final List<Entity> productivities = planningModelGateway.getEntities(
                 createRequest(input, PRODUCTIVITY, utcDateFrom, utcDateTo, null));

        final List<Entity> throughputs = planningModelGateway.getEntities(
                createRequest(input, THROUGHPUT, utcDateFrom, utcDateTo, null));

        final List<Backlog> backlogs = getBacklog.execute(
                new GetBacklogInputDto(input.getWorkflow(), input.getWarehouseId())
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

        return new Projection(
                "Proyecciones",
                new ComplexTable(
                        headers,
                        List.of(createData(config, HEADCOUNT, headcount, headers),
                                createData(config, PRODUCTIVITY, productivities, headers),
                                createData(config, THROUGHPUT, throughputs, headers))
                ),
                createProjectionDetailsTable(
                        backlogs,
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
        return  processingTimeConfiguration.isPresent()
            ? new ProcessingTime(processingTimeConfiguration.get().getValue(),
                    processingTimeConfiguration.get().getMetricUnit().getName())
            : new ProcessingTime(60,MINUTES.getName());
    }

    private SimpleTable createProjectionDetailsTable(
            final List<Backlog> backlogs,
            final List<ProjectionResult> projectionResults,
            final LogisticCenterConfiguration configuration,
            final List<PlanningDistributionResponse> planningDistribution,
            final ProcessingTime processingTime) {

        final ZoneId zoneId = configuration.getTimeZone().toZoneId();

        return new SimpleTable(
                "Resumen de Proyección",
                List.of(
                        new ColumnHeader("column_1", "CPT's"),
                        new ColumnHeader("column_2", "Backlog actual"),
                        new ColumnHeader("column_3", "Desv. vs forecast"),
                        new ColumnHeader("column_4", "Cierre proyectado")
                ),
                projectionResults.stream()
                        .sorted(Comparator.comparing(ProjectionResult::getDate).reversed())
                        .map(projection -> {
                            final ZonedDateTime cpt = projection.getDate();
                            final ZonedDateTime projectedEndDate = projection
                                    .getProjectedEndDate();
                            final int backlog = getBacklogQuantity(cpt, backlogs);

                            return Map.of(
                                    "style", getStyle(cpt, projectedEndDate, processingTime),
                                    "column_1", convertToTimeZone(zoneId, cpt)
                                            .format(CPT_HOUR_FORMAT),
                                    "column_2", String.valueOf(backlog),
                                    "column_3", getDeviation(cpt, backlog, planningDistribution),
                                    "column_4", projectedEndDate == null
                                            ? "+1"
                                            : convertToTimeZone(
                                            zoneId,
                                            projectedEndDate).format(CPT_HOUR_FORMAT)
                            );
                        })
                        .collect(toList())
        );
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
                .filter(distribution -> distribution.getDateOut().equals(cpt))
                .mapToLong(PlanningDistributionResponse::getTotal)
                .sum();

        if (forecastedItemsForCpt == 0 || backlogQuantity == 0) {
            return "0";
        }

        final double deviation = (((double) backlogQuantity / forecastedItemsForCpt) - 1) * 100;

        return String.format("%.2f", Math.round(deviation * 100.00) / 100.00);
    }

    private int getBacklogQuantity(final ZonedDateTime cpt, final List<Backlog> backlogs) {
        final Optional<Backlog> cptBacklog = backlogs.stream()
                .filter(backlog -> backlog.getDate().equals(cpt))
                .findFirst();

        return cptBacklog.map(Backlog::getQuantity).orElse(0);
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

    private List<ColumnHeader> createColumnHeaders(final LogisticCenterConfiguration config,
                                                   final ZonedDateTime utcDateFrom) {

        final ZonedDateTime dateFrom = convertToTimeZone(config.getZoneId(), utcDateFrom);
        final List<ColumnHeader> columns = new ArrayList<>(HOURS_TO_SHOW);

        columns.add(new ColumnHeader("column_1", "Hora de operación"));
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
                            final List<Entity> entities,
                            final List<ColumnHeader> headers) {

        final Map<ProcessName, List<Entity>> entitiesByProcess = entities.stream()
                .collect(groupingBy(Entity::getProcessName));

        final boolean shouldOpenTab = entityType == HEADCOUNT;

        return new Data(
                entityType.getName(),
                capitalize(entityType.getName()),
                shouldOpenTab,
                entitiesByProcess.entrySet().stream()
                        .sorted(Comparator.comparing(entry -> entry.getKey().getIndex()))
                        .map(entry -> createContent(
                                config,
                                entityType, entry.getKey(),
                                headers, entry.getValue()))
                        .collect(toList()));
    }

    private Map<String, Content> createContent(final LogisticCenterConfiguration config,
                                               final EntityType entityType,
                                               final ProcessName processName,
                                               final List<ColumnHeader> headers,
                                               final List<Entity> entities) {

        final Map<String, Map<Source, Entity>> entitiesByHour = entities.stream()
                .map(entity -> entity.convertTimeZone(config.getZoneId()))
                .collect(groupingBy(entity -> getHourAndDay(entity.getDate()),
                        toMap(Entity::getSource, identity())));

        final Map<String, Content> content = new LinkedHashMap<>();

        headers.forEach(header -> {
            if ("column_1".equals(header.getId())) {
                content.put(header.getId(),
                        new Content(capitalize(processName.getName()), null, null));
            } else {
                final Map<Source, Entity> entityBySource = entitiesByHour.get(header.getValue());

                if (entityBySource != null && entityBySource.containsKey(SIMULATION)) {
                    content.put(header.getId(), new Content(
                            valueOf(entityBySource.get(SIMULATION).getValue()),
                            entityBySource.get(SIMULATION).getDate(),
                            createTooltip(entityType, entityBySource.get(FORECAST))));
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

    private Map<String, String> createTooltip(final EntityType entityType, final Entity entity) {
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
                        "subtitle_1", format("%s uds/h", 0));
            default:
                return null;
        }
    }

    private List<ChartData> toChartData(final List<ProjectionResult> projectionResult,
                                        final ZoneId zoneId,
                                        final ZonedDateTime dateTo) {
        return projectionResult.stream()
                .map(projection -> ChartData.fromProjection(
                        convertToTimeZone(zoneId, projection.getDate()),
                        convertToTimeZone(zoneId, projection.getProjectedEndDate() == null
                                ? dateTo : projection.getProjectedEndDate())
                ))
                .collect(Collectors.toList());
    }

    protected abstract List<ProjectionResult> getProjection(final GetProjectionInputDto input,
                                                            final ZonedDateTime dateFrom,
                                                            final ZonedDateTime dateTo,
                                                            final List<Backlog> backlogs);
}
