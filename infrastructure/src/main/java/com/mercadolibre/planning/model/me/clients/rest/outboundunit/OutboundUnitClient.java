package com.mercadolibre.planning.model.me.clients.rest.outboundunit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpClient;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpRequest;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpRequestBuilder;
import com.mercadolibre.fbm.wms.outbound.commons.rest.RequestBodyHandler;
import com.mercadolibre.fbm.wms.outbound.commons.rest.exception.ClientException;
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
import com.mercadolibre.planning.model.me.clients.rest.utils.FailOnExceptionAction;
import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.ProcessBacklog;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.UnitProcessBacklogInput;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogFilters;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.resilience.breaker.CircuitBreaker;
import com.mercadolibre.restclient.MeliRestClient;
import com.mercadolibre.restclient.exception.ParseException;
import com.newrelic.api.agent.Trace;
import lombok.extern.slf4j.Slf4j;
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
import java.util.concurrent.ExecutionException;

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
import static java.util.stream.Collectors.toList;

@Slf4j
@Component
public class OutboundUnitClient extends HttpClient implements BacklogGateway {

    public static final String CLIENT_ID = "9999";
    private static final String SEARCH_GROUPS_URL = "/wms/warehouses/%s/outbound/groups/%s/search";
    private static final String SEARCH_UNITS_URL = "/wms/warehouses/%s/outbound/units/search";
    private static final String AGGREGATION_BY_ETD = "by_etd";
    private static final String ORDER_VALUE = "order";
    private static final String API_NAME = "OUTBOUND_UNIT";

    private final CircuitBreaker unitCircuitBreaker;
    private final ObjectMapper objectMapper;

    protected OutboundUnitClient(final MeliRestClient client,
                                 final ObjectMapper mapper,
                                 final CircuitBreaker unitCircuitBreaker) {
        super(client, RestPool.OUTBOUND_UNIT.name());
        this.objectMapper = mapper;
        this.unitCircuitBreaker = unitCircuitBreaker;
    }

    @Override
    public boolean supportsWorkflow(final Workflow workflow) {
        return Workflow.FBM_WMS_OUTBOUND == workflow;
    }

    @Override
    public List<Backlog> getBacklog(final String warehouseId) {

        final ZonedDateTime dateFrom = getCurrentUtcDate();
        final ZonedDateTime dateTo = dateFrom.plusDays(1).plusHours(1);

        return this.getBacklog(warehouseId, dateFrom, dateTo, List.of("pending"), List.of("etd"));
    }

    @Override
    public List<Backlog> getBacklog(final String warehouseId,
                                    final ZonedDateTime dateFrom,
                                    final ZonedDateTime dateTo,
                                    final List<String> statuses,
                                    final List<String> aggregationKeys) {

        final SearchUnitRequest request = SearchUnitRequest.builder()
                .limit(0)
                .offset(0)
                .filter(createFilters(BacklogFilters.builder()
                        .groupType(ORDER_VALUE)
                        .warehouseId(warehouseId)
                        .statuses(statuses.stream().map(s -> Map.of("status", s)).collect(toList()))
                        .build(), "or"))
                .aggregations(List.of(
                        SearchUnitAggregationRequest.builder()
                                .name(AGGREGATION_BY_ETD)
                                .keys(aggregationKeys)
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

        final OutboundUnitSearchResponse<UnitGroup> response =
                searchGroups(ORDER_VALUE, warehouseId, request, true);

        return response.getAggregations().stream()
                .filter(aggregationResponse ->
                        aggregationResponse.getName().equalsIgnoreCase(AGGREGATION_BY_ETD))
                .map(AggregationResponse::getBuckets)
                .flatMap(Collection::stream)
                .filter(this::validCptKeys)
                .map(this::toBacklog)
                .filter(backlog -> workingCpts(backlog, dateFrom, dateTo))
                .collect(toList());
    }

    @Override
    public List<ProcessBacklog> getBacklog(final List<Map<String, String>> statuses,
                                           final String warehouseId,
                                           final ZonedDateTime dateFrom,
                                           final ZonedDateTime dateTo,
                                           final boolean forceConsistency) {
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

        final OutboundUnitSearchResponse<UnitGroup> response =
                searchGroups(ORDER_VALUE, warehouseId, request, forceConsistency);

        return response.getAggregations().stream()
                .map(AggregationResponse::getBuckets)
                .flatMap(Collection::stream)
                .map(this::toProcessBacklog)
                .collect(toList());
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
        final String warehouseId = input.getWarehouseId();

        addUnitParam(WAREHOUSE_ID.toJson(), warehouseId, defaultParams);
        addUnitParam("group.etd_from", input.getDateFrom().toString(), defaultParams);
        addUnitParam("group.etd_to", input.getDateTo(), defaultParams);
        addUnitParam("group.type", input.getGroupType(), defaultParams);
        addUnitParam(LIMIT.toJson(), "1", defaultParams);
        addUnitParam(STATUS.toJson(), input.getStatuses(), defaultParams);
        addUnitParam("address.area", input.getArea(), defaultParams);

        final OutboundUnitSearchResponse<Unit> response = searchUnits(defaultParams,
                warehouseId, input.isForceConsistency());

        int quantity = Objects.nonNull(response) ? response.getPaging().getTotal() : 0;
        return ProcessBacklog.builder()
                .process(input.getStatuses())
                .quantity(quantity)
                .area(input.getArea())
                .build();
    }

    private void addUnitParam(final String paramLabel,
            final Object param,
            final Map<String, String> defaultParams) {
        ofNullable(param)
                .ifPresent(date -> defaultParams.put(paramLabel, date.toString()));
    }

    private boolean workingCpts(final Backlog backlog,
                                final ZonedDateTime dateFrom,
                                final ZonedDateTime dateTo) {
        final ZonedDateTime backlogCptDate = backlog.getDate();

        return backlogCptDate.isAfter(dateFrom) && backlogCptDate.isBefore(dateTo);
    }

    private boolean validCptKeys(final AggregationResponseBucket bucket) {
        return !bucket.getKeys().contains("undefined");
    }

    private Backlog toBacklog(final AggregationResponseBucket bucket) {
        if (bucket.getKeys().size() > 1) {
            return new Backlog(
                    ZonedDateTime.parse(bucket.getKeys().get(0)),
                    bucket.getKeys().get(1),
                    Math.toIntExact(bucket.getTotals().get(0).getResult()));
        } else {
            return new Backlog(
                    ZonedDateTime.parse(bucket.getKeys().get(0)),
                    Math.toIntExact(bucket.getTotals().get(0).getResult()));
        }
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

        final OutboundUnitSearchResponse<UnitGroup> response =
                searchGroups("order", filters.getWarehouseId(), request, false);

        return response.getAggregations().stream()
                .filter(aggregationResponse ->
                        aggregationResponse.getName().equalsIgnoreCase(AGGREGATION_BY_ETD))
                .map(AggregationResponse::getBuckets)
                .flatMap(Collection::stream)
                .filter(this::validCptKeys)
                .map(this::toBacklog)
                .collect(toList());
    }

    @Trace(metricName = "API/outboundUnit/searchGroups")
    public OutboundUnitSearchResponse<UnitGroup> searchGroups(
            final String groupType,
            final String warehouseId,
            final SearchUnitRequest searchUnitRequest,
            final boolean forceConsistency) {

        final HttpRequestBuilder requestBuilder = HttpRequest.builder()
                .url(format(SEARCH_GROUPS_URL, warehouseId, groupType))
                .POST(requestSupplier(searchUnitRequest))
                .queryParams(defaultParams())
                .acceptedHttpStatuses(Set.of(HttpStatus.OK));

        requestBuilder.headers(Map.of("x-slave", "true"));

        try {
            return unitCircuitBreaker.run(
                    (FailOnExceptionAction<OutboundUnitSearchResponse<UnitGroup>>)
                    () -> send(requestBuilder.build(), response ->
                                    response.getData(new TypeReference<>() {})));
        } catch (ExecutionException e) {
            log.error("An error has occurred on OutboundUnitClient - searchGroups: ", e);
            throw new ClientException(API_NAME, requestBuilder.build(), e.getCause());
        }
    }

    @Trace(metricName = "API/outboundUnit/searchUnits")
    private OutboundUnitSearchResponse<Unit> searchUnits(final Map<String, String> params,
                                                         final String warehouseId,
                                                         final boolean forceConsistency) {

        final HttpRequestBuilder requestBuilder = HttpRequest.builder()
                .url(String.format(SEARCH_UNITS_URL, warehouseId))
                .GET()
                .queryParams(params)
                .acceptedHttpStatuses(Set.of(HttpStatus.OK));

        try {
            return unitCircuitBreaker.run(
                    (FailOnExceptionAction<OutboundUnitSearchResponse<Unit>>)
                    () -> send(requestBuilder.build(), response ->
                            response.getData(new TypeReference<>() {})));
        } catch (ExecutionException e) {
            log.error("An error has occurred on OutboundUnitClient - searchUnits: ", e);
            throw new ClientException(API_NAME, requestBuilder.build(), e.getCause());
        }
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
