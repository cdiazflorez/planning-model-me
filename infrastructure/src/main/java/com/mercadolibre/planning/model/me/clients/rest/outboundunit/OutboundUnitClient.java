package com.mercadolibre.planning.model.me.clients.rest.outboundunit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpClient;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpRequest;
import com.mercadolibre.fbm.wms.outbound.commons.rest.RequestBodyHandler;
import com.mercadolibre.json.type.TypeReference;
import com.mercadolibre.planning.model.me.clients.rest.config.RestPool;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.UnitGroup;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitAggregationFilterRequest;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitAggregationRequest;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitAggregationRequestTotal;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitRequest;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.response.AggregationResponse;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.response.AggregationResponseBucket;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.response.OutboundUnitSearchResponse;
import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.restclient.RestClient;
import com.mercadolibre.restclient.exception.ParseException;
import com.newrelic.api.agent.Trace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitAggregationRequestTotalOperation.SUM;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitFilterRequestStringValue.DATE_CREATED_FROM;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitFilterRequestStringValue.GROUP_TYPE;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitFilterRequestStringValue.STATUS;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitFilterRequestStringValue.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static java.lang.String.format;

@Slf4j
@Component
public class OutboundUnitClient extends HttpClient implements BacklogGateway {

    private static final String SEARCH_GROUPS_URL = "/wms/outbound/groups/%s/search";
    public static final String CLIENT_ID = "9999";
    private static final String AGGREGATION_BY_ETD = "by_etd";

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
                .filter(new SearchUnitAggregationFilterRequest(List.of(
                        Map.of(WAREHOUSE_ID.toJson(), warehouseId),
                        Map.of(GROUP_TYPE.toJson(), "order"),
                        Map.of(STATUS.toJson(), "pending")
                )))
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

        final OutboundUnitSearchResponse<UnitGroup> response = searchGroups("order", request);

        return response.getAggregations().stream()
                .filter(aggregationResponse ->
                        aggregationResponse.getName().equalsIgnoreCase(AGGREGATION_BY_ETD))
                .map(AggregationResponse::getBuckets)
                .flatMap(aggregationResponseBuckets -> aggregationResponseBuckets.stream())
                .filter(this::validCptKeys)
                .map(this::toBacklog)
                .filter(this::workingCpts)
                .collect(Collectors.toList());
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

    @Override
    public List<Backlog> getSalesByCpt(final String warehouseId, final String dateCreatedFrom) {
        final SearchUnitRequest request = SearchUnitRequest.builder()
                .limit(0)
                .offset(0)
                .filter(new SearchUnitAggregationFilterRequest(List.of(
                        Map.of(WAREHOUSE_ID.toJson(), warehouseId),
                        Map.of(GROUP_TYPE.toJson(), "order"),
                        Map.of(DATE_CREATED_FROM.toJson(), dateCreatedFrom)
                )))
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
                .flatMap(aggregationResponseBuckets -> aggregationResponseBuckets.stream())
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

        return send(request, response -> response.getData(new TypeReference<>() {}));
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
