package com.mercadolibre.planning.model.me.clients.rest.outboundunit;

import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpClient;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpRequest;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpRequestBuilder;
import com.mercadolibre.fbm.wms.outbound.commons.rest.exception.ClientException;
import com.mercadolibre.json.type.TypeReference;
import com.mercadolibre.planning.model.me.clients.rest.config.RestPool;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.Unit;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.response.OutboundUnitSearchResponse;
import com.mercadolibre.planning.model.me.clients.rest.utils.FailOnExceptionAction;
import com.mercadolibre.planning.model.me.entities.projection.ProcessBacklog;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.UnitProcessBacklogInput;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.resilience.breaker.CircuitBreaker;
import com.mercadolibre.restclient.MeliRestClient;
import com.newrelic.api.agent.Trace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ExecutionException;

import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitFilterRequestStringValue.*;
import static java.util.Optional.ofNullable;

@Slf4j
@Component
public class OutboundUnitClient extends HttpClient implements BacklogGateway {

    public static final String CLIENT_ID = "9999";
    private static final String SEARCH_UNITS_URL = "/wms/warehouses/%s/outbound/units/search";
    private static final String API_NAME = "OUTBOUND_UNIT";

    private final CircuitBreaker unitCircuitBreaker;

    protected OutboundUnitClient(final MeliRestClient client,
                                 final CircuitBreaker unitCircuitBreaker) {
        super(client, RestPool.OUTBOUND_UNIT.name());
        this.unitCircuitBreaker = unitCircuitBreaker;
    }

    @Override
    public boolean supportsWorkflow(final Workflow workflow) {
        return Workflow.FBM_WMS_OUTBOUND == workflow;
    }

    @Trace
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

        final OutboundUnitSearchResponse<Unit> response = searchUnits(defaultParams, warehouseId);

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

    @Trace(metricName = "API/outboundUnit/searchUnits")
    private OutboundUnitSearchResponse<Unit> searchUnits(final Map<String, String> params,
                                                         final String warehouseId) {

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

}
