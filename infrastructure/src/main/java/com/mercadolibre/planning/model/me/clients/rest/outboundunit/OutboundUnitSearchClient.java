package com.mercadolibre.planning.model.me.clients.rest.outboundunit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpClient;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpRequest;
import com.mercadolibre.fbm.wms.outbound.commons.rest.RequestBodyHandler;
import com.mercadolibre.json.type.TypeReference;
import com.mercadolibre.planning.model.me.clients.rest.config.RestPool;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.UnitReportAggregationRequest;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.UnitSearchFilters;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.UnitSearchReportAggregationRequest;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.response.UnitReportAggregationResponse;
import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogFilters;
import com.mercadolibre.planning.model.me.gateways.outboundunit.UnitSearchGateway;
import com.mercadolibre.restclient.MeliRestClient;
import com.mercadolibre.restclient.exception.ParseException;
import com.newrelic.api.agent.Trace;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.ReportAggregationsKeys.GROUP_ESTIMATED_TIME_DEPARTURE;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.ReportAggregationsTotals.TOTAL_ORDERS;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.UnitSearchFilters.GROUP_DATE_CREATED_FROM;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.UnitSearchFilters.GROUP_DATE_CREATED_TO;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.UnitSearchFilters.GROUP_ETD_FROM;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.UnitSearchFilters.GROUP_ETD_TO;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.UnitSearchFilters.GROUP_TYPE;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.UnitSearchFilters.GROUP_WAREHOUSE_ID;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;

@Component
public class OutboundUnitSearchClient extends HttpClient implements UnitSearchGateway {

    private static final String AGGREGATION_BY_ETD = "by_etd";
    private static final String CLIENT_ID = "9999";
    private static final String SEARCH_REPORTS_URL = "/wms/warehouses/%s/outbound/units/search";


    private final ObjectMapper objectMapper;

    protected OutboundUnitSearchClient(MeliRestClient client, ObjectMapper objectMapper) {
        super(client, RestPool.OUTBOUND_UNIT_SEARCH.name());
        this.objectMapper = objectMapper;
    }

    @Trace
    @Override
    public List<Backlog> getSalesByCpt(final BacklogFilters filters) {
        final UnitSearchReportAggregationRequest request =
                UnitSearchReportAggregationRequest.builder()
                        .paging(Map.of("limit", "1"))
                        .filters(createFilters(filters))
                        .aggregations(List.of(UnitReportAggregationRequest.builder()
                                .name(AGGREGATION_BY_ETD)
                                .key(GROUP_ESTIMATED_TIME_DEPARTURE)
                                .totals(List.of(TOTAL_ORDERS))
                        .build()))
                .build();
        final HttpRequest httpRequest = HttpRequest.builder()
                .url(format(SEARCH_REPORTS_URL, filters.getWarehouseId()))
                .POST(requestSupplier(request))
                .queryParams(defaultParams())
                .acceptedHttpStatuses(Set.of(HttpStatus.OK))
                .build();

        UnitReportAggregationResponse reportResponse = send(httpRequest,
                response -> response.getData(new TypeReference<>() {}));
        return reportResponse.mapToBacklog(AGGREGATION_BY_ETD);
    }

    private Map<String, Object> createFilters(final BacklogFilters filters) {
        final Map<String, Object> unitSearchFilters = new HashMap<>();

        addFilter(GROUP_WAREHOUSE_ID, filters.getWarehouseId(), unitSearchFilters);
        addFilter(GROUP_TYPE, "order", unitSearchFilters);
        addFilter(GROUP_ETD_FROM, filters.getCptFrom(), unitSearchFilters);
        addFilter(GROUP_ETD_TO, filters.getCptTo(), unitSearchFilters);
        addFilter(GROUP_DATE_CREATED_FROM, filters.getDateCreatedFrom(), unitSearchFilters);
        addFilter(GROUP_DATE_CREATED_TO, filters.getDateCreatedTo(), unitSearchFilters);

        return unitSearchFilters;
    }

    private void addFilter(final UnitSearchFilters filterLabel,
                           final Object filter,
                           final Map<String, Object> filters) {

        ofNullable(filter).ifPresent(currentFilter ->
                filters.put(filterLabel.getFieldName(), currentFilter));
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
