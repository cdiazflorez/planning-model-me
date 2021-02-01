package com.mercadolibre.planning.model.me.clients.rest.outboundunit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpClient;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpRequest;
import com.mercadolibre.fbm.wms.outbound.commons.rest.RequestBodyHandler;
import com.mercadolibre.json.type.TypeReference;
import com.mercadolibre.planning.model.me.clients.rest.config.RestPool;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.Unit;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.UnitGroup;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitAggregationFilterRequest;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitAggregationRequest;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitAggregationRequestTotal;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitRequest;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.response.AggregationResponse;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.response.AggregationResponseBucket;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.response.OutboundUnitSearchResponse;
import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.ProcessBacklog;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.UnitProcessBacklogInput;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogFilters;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.restclient.RestClient;
import com.mercadolibre.restclient.exception.ParseException;
import com.newrelic.api.agent.Trace;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitAggregationRequestTotalOperation.SUM;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitFilterRequestStringValue.DATE_CREATED_FROM;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitFilterRequestStringValue.DATE_CREATED_TO;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitFilterRequestStringValue.ETD_FROM;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitFilterRequestStringValue.ETD_TO;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitFilterRequestStringValue.GROUP_TYPE;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitFilterRequestStringValue.LIMIT;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitFilterRequestStringValue.STATUS;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitFilterRequestStringValue.WAREHOUSE_ID;

import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;

@Component
public class OutboundUnitClient extends HttpClient implements BacklogGateway {

    private static final String SEARCH_GROUPS_URL = "/wms/outbound/groups/%s/search";
    private static final String SEARCH_UNITS_URL = "/wms/outbound/units/search";
    public static final String CLIENT_ID = "9999";
    private static final String AGGREGATION_BY_ETD = "by_etd";
    private static final String ORDER_VALUE = "order";


    private final ObjectMapper objectMapper;

    protected OutboundUnitClient(final RestClient client, final ObjectMapper mapper) {
        super(client, RestPool.OUTBOUND_UNIT.name());
        this.objectMapper = mapper;
    }

    @Override
    public boolean supportsWorkflow(final Workflow workflow) {
        return Workflow.FBM_WMS_OUTBOUND == workflow;
    }

    @Override
    public List<Backlog> getBacklog(final String warehouseId) {
        final SearchUnitRequest request = SearchUnitRequest.builder()
                .limit(0)
                .offset(0)
                .filter(createFilters(BacklogFilters.builder()
                        .groupType(ORDER_VALUE)
                        .warehouseId(warehouseId)
                        .statuses("pending")
                        .build(), STATUS.toJson()))
                .aggregations(List.of(
                        SearchUnitAggregationRequest.builder()
                                .name(AGGREGATION_BY_ETD)
                                .keys(List.of("etd"))
                                .totals(List.of(
                                        SearchUnitAggregationRequestTotal.builder()
                                                .alias("total_units")
                                                .operand("$quantity")
                                                .operation(SUM)
                                                .build()
                                ))
                                .build()
                ))
                .build();

        final OutboundUnitSearchResponse<UnitGroup> response = searchGroups(ORDER_VALUE, request);

        return response.getAggregations().stream()
                .filter(aggregationResponse ->
                        aggregationResponse.getName().equalsIgnoreCase(AGGREGATION_BY_ETD))
                .map(AggregationResponse::getBuckets)
                .flatMap(Collection::stream)
                .filter(this::validCptKeys)
                .map(this::toBacklog)
                .filter(this::workingCpts)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProcessBacklog> getBacklog(final List<Map<String, String>> statuses,
                                           final String warehouseId,
                                           final ZonedDateTime dateFrom,
                                           final ZonedDateTime dateTo) {
        final SearchUnitRequest request = SearchUnitRequest.builder()
                .limit(1)
                .offset(0)
                .filter(createFilters(BacklogFilters.builder()
                        .cptFrom(dateFrom)
                        .cptTo(dateTo)
                        .statuses(statuses)
                        .warehouseId(warehouseId)
                        .groupType(ORDER_VALUE)
                        .build(),"or"))
                .aggregations(List.of(
                        SearchUnitAggregationRequest.builder()
                                .name(STATUS.toJson())
                                .keys(List.of(STATUS.toJson()))
                                .totals(List.of(
                                        SearchUnitAggregationRequestTotal.builder()
                                                .alias("total_units")
                                                .operand("$quantity")
                                                .operation(SUM)
                                                .build()
                                ))
                                .build()
                ))
                .build();

        final OutboundUnitSearchResponse<UnitGroup> response = searchGroups(ORDER_VALUE, request);

        return response.getAggregations().stream()
                .map(AggregationResponse::getBuckets)
                .flatMap(Collection::stream)
                .map(this::toProcessBacklog)
                .collect(Collectors.toList());
    }

    private SearchUnitAggregationFilterRequest createFilters(
            final BacklogFilters filters,
            final String statusLabel) {
        final List<Map<String,Object>> filtersList = new LinkedList<>();
        addFilter(WAREHOUSE_ID.toJson(), filters.getWarehouseId(), filtersList);
        addFilter(GROUP_TYPE.toJson(), filters.getGroupType(), filtersList);
        addFilter(ETD_FROM.toJson(), filters.getCptFrom(), filtersList);
        addFilter(ETD_TO.toJson(), filters.getCptTo(), filtersList);
        addFilter(DATE_CREATED_FROM.toJson(), filters.getDateCreatedFrom(), filtersList);
        addFilter(DATE_CREATED_TO.toJson(), filters.getDateCreatedTo(), filtersList);

        addFilter(statusLabel, filters.getStatuses(), filtersList);
        return new SearchUnitAggregationFilterRequest(filtersList);
    }

    private void addFilter(final String filterLabel,
                           final Object filter,
                           List<Map<String, Object>> filters) {
        ofNullable(filter).ifPresent(currentFilter ->
                filters.add(Map.of(filterLabel, currentFilter)));
    }

    @Override
    public ProcessBacklog getUnitBacklog(final UnitProcessBacklogInput input) {
        final Map<String, String> defaultParams = defaultParams();

        addUnitParam(WAREHOUSE_ID.toJson(), input.getWarehouseId(), defaultParams);
        addUnitParam("group.etd_from", input.getDateFrom().toString(), defaultParams);
        addUnitParam("group.etd_to", input.getDateTo(), defaultParams);
        addUnitParam(LIMIT.toJson(), "1", defaultParams);
        addUnitParam(STATUS.toJson(), input.getStatuses(), defaultParams);
        addUnitParam("address.area", input.getArea(), defaultParams);

        final OutboundUnitSearchResponse<Unit> response = searchUnits(defaultParams);
        int quantity = Objects.nonNull(response) ? response.getPaging().getTotal() : 0;
        return ProcessBacklog.builder()
                .process(input.getStatuses())
                .quantity(quantity)
                .area(input.getArea())
                .build();
    }

    public void addUnitParam(final String paramLabel,
            final Object param,
            final Map<String, String> defaultParams) {
        ofNullable(param)
                .ifPresent(date -> defaultParams.put(paramLabel, date.toString()));
    }

    private boolean workingCpts(final Backlog backlog) {
        final ZonedDateTime currentTime = getCurrentUtcDate();
        final ZonedDateTime backlogCptDate = backlog.getDate();

        return backlogCptDate.isAfter(currentTime)
                && backlogCptDate.isBefore(currentTime.plusDays(1).plusHours(1));
    }

    private boolean validCptKeys(final AggregationResponseBucket bucket) {
        return !bucket.getKeys().contains("undefined");
    }

    private Backlog toBacklog(final AggregationResponseBucket bucket) {
        return Backlog.builder()
                .date(ZonedDateTime.parse(bucket.getKeys().get(0)))
                .quantity(Math.toIntExact(bucket.getTotals().get(0).getResult()))
                .build();
    }

    private ProcessBacklog toProcessBacklog(final AggregationResponseBucket bucket) {
        return ProcessBacklog.builder()
                .process(bucket.getKeys().get(0))
                .quantity(Math.toIntExact(bucket.getTotals().get(0).getResult()))
                .build();
    }

    @Override
    public List<Backlog> getSalesByCpt(final BacklogFilters filters) {
        final SearchUnitRequest request = SearchUnitRequest.builder()
                .limit(0)
                .offset(0)
                .filter(createFilters(filters,null))
                .aggregations(List.of(
                        SearchUnitAggregationRequest.builder()
                                .name(AGGREGATION_BY_ETD)
                                .keys(List.of("etd"))
                                .totals(List.of(
                                        SearchUnitAggregationRequestTotal.builder()
                                                .alias("sales")
                                                .operand("$quantity")
                                                .operation(SUM)
                                                .build()
                                ))
                                .build()
                ))
                .build();

        final OutboundUnitSearchResponse<UnitGroup> response = searchGroups("order", request);

        return response.getAggregations().stream()
                .filter(aggregationResponse ->
                        aggregationResponse.getName().equalsIgnoreCase(AGGREGATION_BY_ETD))
                .map(AggregationResponse::getBuckets)
                .flatMap(Collection::stream)
                .filter(this::validCptKeys)
                .map(this::toBacklog)
                .collect(Collectors.toList());
    }

    @Trace(metricName = "API/outboundUnit/searchGroups")
    public OutboundUnitSearchResponse<UnitGroup> searchGroups(
            final String groupType,
            final SearchUnitRequest searchUnitRequest) {

        final HttpRequest request = HttpRequest.builder()
                .url(format(SEARCH_GROUPS_URL, groupType))
                .POST(requestSupplier(searchUnitRequest))
                .queryParams(defaultParams())
                .acceptedHttpStatuses(Set.of(HttpStatus.OK))
                .build();

        return send(request, response -> response.getData(new TypeReference<>() {
        }));
    }

    @Trace(metricName = "API/outboundUnit/searchUnits")
    public OutboundUnitSearchResponse<Unit> searchUnits(final Map<String, String> params) {

        final HttpRequest request = HttpRequest.builder()
                .url(SEARCH_UNITS_URL)
                .GET()
                .queryParams(params)
                .acceptedHttpStatuses(Set.of(HttpStatus.OK))
                .build();

        return send(request, response -> response.getData(new TypeReference<>() {
        }));
    }

    private Map<String, String> defaultParams() {
        final Map<String, String> defaultParams = new HashMap<>();
        defaultParams.put("client.id", CLIENT_ID);
        return defaultParams;
    }

    private <T> RequestBodyHandler requestSupplier(final T requestBody) {
        return () -> {
            try {
                return objectMapper.writeValueAsBytes(requestBody);
            } catch (JsonProcessingException e) {
                throw new ParseException(e);
            }
        };
    }
}
