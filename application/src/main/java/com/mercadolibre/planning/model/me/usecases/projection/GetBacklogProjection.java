package com.mercadolibre.planning.model.me.usecases.projection;

import com.mercadolibre.planning.model.me.entities.projection.BacklogProjection;
import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.ProcessBacklog;
import com.mercadolibre.planning.model.me.entities.projection.SelectionValue;
import com.mercadolibre.planning.model.me.entities.projection.Selections;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.exception.BacklogGatewayNotSupportedException;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.UnitProcessBacklogInput;
import com.mercadolibre.planning.model.me.gateways.backlog.strategy.BacklogGatewayProvider;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.CurrentBacklog;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.ProjectionValue;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklog;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.BacklogProjectionInput;
import com.mercadolibre.planning.model.me.utils.ResponseUtils;
import lombok.AllArgsConstructor;

import javax.inject.Named;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.REMAINING_PROCESSING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.BacklogProjectionRequest.fromInput;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo.OUTBOUND_PLANNING;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getHourAndDay;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getNextHour;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.createColumnHeaders;
import static java.util.stream.Collectors.toMap;

@Named
@AllArgsConstructor
public class GetBacklogProjection implements UseCase<BacklogProjectionInput, BacklogProjection> {

    private static final int HOURS_TO_SHOW = 25;

    protected final PlanningModelGateway planningModel;

    protected final LogisticCenterGateway logisticCenter;

    protected final GetBacklog getBacklog;

    private final BacklogGatewayProvider backlogGatewayProvider;

    @Override
    public BacklogProjection execute(final BacklogProjectionInput input) {
        final List<CurrentBacklog> backlogs = getBacklogList(input);
        final List<BacklogProjectionResponse> projections = planningModel
                .getBacklogProjection(fromInput(input, backlogs));

        return mapResponse(input, projections);
    }

    private List<CurrentBacklog> getBacklogList(final BacklogProjectionInput input) {
        final ZonedDateTime dateFrom = input.getDateFrom();
        final List<Map<String, String>> statuses = List.of(
                Map.of("status", OUTBOUND_PLANNING.getStatus()),
                Map.of("status", ProcessInfo.PACKING.getStatus())
        );

        final BacklogGateway backlogGateway = backlogGatewayProvider.getBy(input.getWorkflow())
                .orElseThrow(() -> new BacklogGatewayNotSupportedException(input.getWorkflow()));

        List<ProcessBacklog> backlogs =
                backlogGateway.getBacklog(statuses,
                        input.getWarehouseId(),
                        dateFrom,
                        dateFrom.plusHours(HOURS_TO_SHOW)
                );

        final ProcessBacklog pickingBacklog =
                backlogGateway.getUnitBacklog(
                        new UnitProcessBacklogInput(ProcessInfo.PICKING.getStatus(),
                                input.getWarehouseId(),
                                dateFrom,
                                dateFrom.plusHours(HOURS_TO_SHOW),
                                null,
                                input.getGroupType())
                );

        return List.of(
                new CurrentBacklog(WAVING, backlogs.stream()
                        .filter(t -> t.getProcess().equals(OUTBOUND_PLANNING.getStatus()))
                        .findFirst()
                        .map(ProcessBacklog::getQuantity)
                        .orElse(0)
                ),
                new CurrentBacklog(PICKING, pickingBacklog.getQuantity()),
                new CurrentBacklog(PACKING, backlogs.stream()
                        .filter(t -> t.getProcess().equals(ProcessInfo.PACKING.getStatus()))
                        .findFirst()
                        .map(ProcessBacklog::getQuantity)
                        .orElse(0))
        );
    }

    private BacklogProjection mapResponse(final BacklogProjectionInput input,
                                          final List<BacklogProjectionResponse> projections) {
        final ZonedDateTime dateFrom = input.getDateFrom();
        final ZoneId zoneId = logisticCenter.getConfiguration(input.getWarehouseId()).getZoneId();
        final List<ColumnHeader> headers = createColumnHeaders(
                convertToTimeZone(zoneId, getNextHour(dateFrom)), HOURS_TO_SHOW);

        final List<Entity> remainingProcessing = planningModel.getEntities(EntityRequest.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .processName(List.of(PICKING))
                .entityType(REMAINING_PROCESSING)
                .dateFrom(dateFrom)
                .dateTo(dateFrom.plusHours(HOURS_TO_SHOW))
                .build());

        return com.mercadolibre.planning.model.me.entities.projection.BacklogProjection.builder()
                .title("Proyecciones")
                .tabs(ResponseUtils.createTabs())
                .selections(createSelections(input.getProcessName()))
                .simpleTable1(createWavingTable(
                        zoneId, headers, projections.get(0), remainingProcessing))
                .simpleTable2(createProcessTable(zoneId, headers, projections))
                .build();
    }

    private Selections createSelections(final List<ProcessName> processNames) {
        return new Selections("Procesos", List.of(
                new SelectionValue(
                        PICKING.getName(),
                        "Ready to pick",
                        processNames.contains(PICKING)),
                new SelectionValue(
                        PACKING.getName(),
                        "Ready to pack",
                        processNames.contains(PACKING))
        ));
    }

    private SimpleTable createWavingTable(final ZoneId zoneId,
                                          final List<ColumnHeader> headers,
                                          final BacklogProjectionResponse projection,
                                          final List<Entity> remainingProcessing) {
        return new SimpleTable("Ready to wave FCST vs Real", headers, List.of(
                createTableValues(
                        zoneId,
                        Map.of("title","FCST","subtitle","(uds.)"),
                        headers,
                        remainingProcessing.stream()
                                .map(ProjectionValue::fromEntity)
                                .collect(Collectors.toList())
                ),
                createTableValues(
                        zoneId,
                        Map.of("title","Real","subtitle","(uds.)"),
                        headers,
                        projection.getValues()))
        );
    }

    private SimpleTable createProcessTable(final ZoneId zoneId,
                                           final List<ColumnHeader> headers,
                                           final List<BacklogProjectionResponse> projections) {

        final Optional<BacklogProjectionResponse> pickingProjection = projections.stream()
                .filter(projection -> PICKING.equals(projection.getProcessName()))
                .findFirst();
        final Optional<BacklogProjectionResponse> packingProjection = projections.stream()
                .filter(projection -> PACKING.equals(projection.getProcessName()))
                .findFirst();

        final List<Map<String, Object>> data = new ArrayList<>();
        pickingProjection.ifPresent(projection ->
                data.add(createTableValues(
                        zoneId,
                        Map.of("title", "RTPick", "id", "ready_to_pick", "subtitle", "(uds.)"),
                        headers,
                        projection.getValues())));

        packingProjection.ifPresent(projection ->
                data.add(createTableValues(
                        zoneId,
                        Map.of("title", "RTPack", "id", "ready_to_pack", "subtitle", "(uds.)"),
                        headers,
                        projection.getValues())));

        return new SimpleTable("Gráfico de proyección de backlogs", headers, data);
    }

    private Map<String, Object> createTableValues(final ZoneId zoneId,
                                                  final Map<String, String> firstElement,
                                                  final List<ColumnHeader> headers,
                                                  final List<ProjectionValue> projectionValues) {

        final Map<String, String> quantityByDateTime = projectionValues.stream().collect(toMap(
                o -> getHourAndDay(convertToTimeZone(zoneId, o.getDate())),
                o -> String.valueOf(o.getQuantity()),
                (o1, o2) -> o2));
        final Map<String, Object> values = new LinkedHashMap<>();

        values.put(headers.get(0).getId(), firstElement);
        headers.stream().skip(1).forEach(header ->
                values.put(header.getId(), quantityByDateTime.getOrDefault(
                        header.getValue(), "0"))
        );
        return values;
    }
}
