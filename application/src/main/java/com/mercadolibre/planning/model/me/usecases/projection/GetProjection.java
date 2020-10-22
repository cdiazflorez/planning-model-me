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

import java.time.LocalDateTime;
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
        final String warehouseId = input.getWarehouseId();
        final Workflow workflow = input.getWorkflow();
        final LogisticCenterConfiguration config =
                logisticCenterGateway.getConfiguration(warehouseId);

        final List<Entity> headcount = planningModelGateway.getEntities(
                createRequest(workflow, warehouseId, HEADCOUNT));
        final List<Entity> productivity = planningModelGateway.getEntities(
                createRequest(workflow, warehouseId, PRODUCTIVITY));
        final List<Entity> throughput =  planningModelGateway.getEntities(
                createRequest(workflow, warehouseId, THROUGHPUT));

        final List<ProjectionResult> projections = getProjections(input);
        final List<ColumnHeader> headers = createColumnHeaders(config);

        return new Projection(
                "Proyecciones",
                new ComplexTable(
                        headers,
                        List.of(createData(config, HEADCOUNT, headcount, headers),
                                createData(config, PRODUCTIVITY, productivity, headers),
                                createData(config, THROUGHPUT, throughput, headers))
                ),
                // TODO: Get processing time from /configuration endpoint in PlanningModelApiClient
                new Chart(
                        new ProcessingTime(60, "minutes"),
                        projections.stream()
                                .map(projectionResult -> ChartData.fromProjectionResponse(
                                        projectionResult, config.getTimeZone().toZoneId())
                                )
                                .collect(toList())
                )
        );
    }

    private EntityRequest createRequest(final Workflow workflow,
                                        final String warehouseId,
                                        final EntityType entityType) {
        final ZonedDateTime currentTime = getCurrentTime();

        return EntityRequest.builder()
                .workflow(workflow)
                .warehouseId(warehouseId)
                .entityType(entityType)
                .dateFrom(currentTime)
                .dateTo(currentTime.plusDays(1))
                .processName(PROJECTION_PROCESS_NAMES)
                .build();
    }

    private List<ProjectionResult> getProjections(final GetProjectionInputDto input) {
        final ZonedDateTime currentTime = getCurrentTime();
        final List<Backlog> backlogs = getBacklog.execute(
                new GetBacklogInputDto(input.getWorkflow(), input.getWarehouseId())
        );

        return planningModelGateway.runProjection(ProjectionRequest.builder()
                .warehouseId(input.getWarehouseId())
                .workflow(input.getWorkflow())
                .processName(List.of(PICKING, PACKING))
                .type(CPT)
                .dateFrom(currentTime)
                .dateTo(currentTime.plusHours(HOURS_TO_SHOW))
                .backlog(backlogs)
                .build());

    }

    private List<ColumnHeader> createColumnHeaders(final LogisticCenterConfiguration config) {
        final LocalDateTime actualTime = config.getLocalDateTime();

        final List<ColumnHeader> columns = new ArrayList<>(25);
        columns.add(new ColumnHeader("column_1", "Hora de operacion", null));

        columns.addAll(IntStream.range(0, HOURS_TO_SHOW)
                .mapToObj(index -> new ColumnHeader(
                        format("column_%s", 2 + index),
                        actualTime.plusHours(index).format(HOUR_FORMAT),
                        actualTime.plusHours(index).getHour()
                                + "-"
                                + actualTime.plusHours(index).getDayOfMonth()))
                .collect(toList()));

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
                .collect(groupingBy(entity -> entity.getHourAndDay(config.getTimeZone()),
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

    private ZonedDateTime getCurrentTime() {
        return ZonedDateTime.now().withMinute(0).withSecond(0).withNano(0);
    }

}
