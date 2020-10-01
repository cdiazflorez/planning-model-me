package com.mercadolibre.planning.model.me.usecases.projection;

import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.ComplexTable;
import com.mercadolibre.planning.model.me.entities.projection.Content;
import com.mercadolibre.planning.model.me.entities.projection.Data;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.UseCase;
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
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source.FORECAST;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source.SIMULATION;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Named
@AllArgsConstructor
public class GetProjection implements UseCase<GetProjectionInputDto, Projection> {

    private static final DateTimeFormatter HOUR_FORMAT = ofPattern("HH:00");
    private static final List<ProcessName> PROJECTION_PROCESS_NAMES = List.of(PICKING, PACKING);
    private static final int HOURS_TO_SHOW = 25;

    private PlanningModelGateway planningModelGateway;

    private LogisticCenterGateway logisticCenterGateway;

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

        return createProjection(headcount, productivity, throughput, config);
    }

    private EntityRequest createRequest(final Workflow workflow,
                                        final String warehouseId,
                                        final EntityType entityType) {
        return EntityRequest.builder()
                .workflow(workflow)
                .warehouseId(warehouseId)
                .entityType(entityType)
                .dateFrom(createNowDate())
                .processName(PROJECTION_PROCESS_NAMES)
                .dateTo(createNowDate().plusDays(1))
                .build();
    }

    private Projection createProjection(final List<Entity> headcount,
                                        final List<Entity> productivity,
                                        final List<Entity> throughput,
                                        final LogisticCenterConfiguration config) {
        final List<ColumnHeader> headers = createColumnHeaders(config);

        return new Projection(
                "Proyecciones",
                new ComplexTable(
                        headers,
                        List.of(createData(HEADCOUNT, headcount, headers),
                                createData(PRODUCTIVITY, productivity, headers),
                                createData(THROUGHPUT, throughput, headers))
                )
        );
    }

    private List<ColumnHeader> createColumnHeaders(final LogisticCenterConfiguration config) {
        final LocalDateTime actualTime = LocalDateTime.now(config.getTimeZone().toZoneId());

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

    private Data createData(final EntityType entityType,
                            final List<Entity> entities,
                            final List<ColumnHeader> headers) {

        final Map<ProcessName, List<Entity>> entitiesByProcess = entities.stream()
                .collect(groupingBy(Entity::getProcessName));

        return new Data(
                entityType.getName(),
                entityType.getName(),
                true,
                entitiesByProcess.entrySet().stream()
                        .sorted(Comparator.comparing(entry -> entry.getKey().getIndex()))
                        .map(entry -> createContent(
                                entityType, entry.getKey(),
                                headers, entry.getValue()))
                        .collect(toList()));
    }

    private Map<String, Content> createContent(final EntityType entityType,
                                               final ProcessName processName,
                                               final List<ColumnHeader> headers,
                                               final List<Entity> entities) {

        final Map<String, Map<Source, Entity>> entitiesByHour = entities.stream()
                .collect(groupingBy(Entity::getHourAndDay, toMap(Entity::getSource, identity())));

        final Map<String, Content> content = new LinkedHashMap<>();

        headers.forEach(header -> {
            if ("column_1".equals(header.getId())) {
                content.put(header.getId(), new Content(processName.getName(), null, null));
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

    private ZonedDateTime createNowDate() {
        return ZonedDateTime.now().withMinute(0).withSecond(0).withNano(0);
    }
}
