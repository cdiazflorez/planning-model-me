package com.mercadolibre.planning.model.me.usecases.projection;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.ComplexTable;
import com.mercadolibre.planning.model.me.entities.projection.Content;
import com.mercadolibre.planning.model.me.entities.projection.Data;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.ProjectionResult;
import com.mercadolibre.planning.model.me.entities.projection.chart.Chart;
import com.mercadolibre.planning.model.me.entities.projection.chart.ChartData;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklog;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogInputDto;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import lombok.AllArgsConstructor;

import javax.inject.Named;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionType.CPT;
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

@Named
@AllArgsConstructor
public class GetProjection implements UseCase<GetProjectionInputDto, Projection> {

    private static final DateTimeFormatter HOUR_FORMAT = ofPattern("HH:00");
    private static final List<ProcessName> PROJECTION_PROCESS_NAMES = List.of(PICKING, PACKING);
    private static final int HOURS_TO_SHOW = 25;

    private final PlanningModelGateway planningModelGateway;
    private final LogisticCenterGateway logisticCenterGateway;
    private final GetBacklog getBacklog;

    @Override
    public Projection execute(final GetProjectionInputDto input) {
        final ZonedDateTime utcDateFrom = getCurrentUtcDate();
        final ZonedDateTime utcDateTo = utcDateFrom.plusDays(1);

        final List<Entity> headcount = planningModelGateway.getEntities(
                createRequest(input, HEADCOUNT, utcDateFrom, utcDateTo));

        final List<Entity> productivities = planningModelGateway.getEntities(
                createRequest(input, PRODUCTIVITY, utcDateFrom, utcDateTo));

        final List<Entity> throughputs = planningModelGateway.getEntities(
                createRequest(input, THROUGHPUT, utcDateFrom, utcDateTo));

        final List<ProjectionResult> projections = getProjections(input, utcDateFrom, utcDateTo);

        final LogisticCenterConfiguration config = logisticCenterGateway.getConfiguration(
                input.getWarehouseId());

        final List<ColumnHeader> headers = createColumnHeaders(config, utcDateFrom);

        return new Projection(
                "Proyecciones",
                new ComplexTable(
                        headers,
                        List.of(createData(config, HEADCOUNT, headcount, headers),
                                createData(config, PRODUCTIVITY, productivities, headers),
                                createData(config, THROUGHPUT, throughputs, headers))
                ),
                // TODO: Get processing time from /configuration endpoint in PlanningModelApiClient
                new Chart(
                        new ProcessingTime(60, "minutes"),
                        projections.stream()
                                .map(projectionResult -> ChartData.fromProjectionResponse(
                                        projectionResult, config.getZoneId(), utcDateTo)
                                )
                                .collect(toList())
                )
        );
    }

    private EntityRequest createRequest(final GetProjectionInputDto input,
                                        final EntityType entityType,
                                        final ZonedDateTime dateFrom,
                                        final ZonedDateTime dateTo) {
        return EntityRequest.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .entityType(entityType)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .processName(PROJECTION_PROCESS_NAMES)
                .build();
    }

    private List<ProjectionResult> getProjections(final GetProjectionInputDto input,
                                                  final ZonedDateTime dateFrom,
                                                  final ZonedDateTime dateTo) {

        final String warehouseId = input.getWarehouseId();
        final Workflow workflow = input.getWorkflow();
        final List<Backlog> backlogs = getBacklog.execute(
                new GetBacklogInputDto(workflow, warehouseId)
        );

        return planningModelGateway.runProjection(ProjectionRequest.builder()
                .warehouseId(warehouseId)
                .workflow(workflow)
                .processName(PROJECTION_PROCESS_NAMES)
                .type(CPT)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .backlog(backlogs)
                .build());
    }

    private List<ColumnHeader> createColumnHeaders(final LogisticCenterConfiguration config,
                                                   final ZonedDateTime utcDateFrom) {

        final ZonedDateTime dateFrom = convertToTimeZone(config.getZoneId(), utcDateFrom);
        final List<ColumnHeader> columns = new ArrayList<>(HOURS_TO_SHOW);

        columns.add(new ColumnHeader("column_1", "Hora de operación", null));
        columns.addAll(IntStream.range(0, HOURS_TO_SHOW)
                .mapToObj(index -> {
                    final ZonedDateTime date = dateFrom.plusHours(index);
                    return new ColumnHeader(
                            format("column_%s", 2 + index),
                            date.format(HOUR_FORMAT),
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
                                entity.getTime().format(HOUR_FORMAT),
                                entity.getTime().plusHours(1).format(HOUR_FORMAT)),
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
}
