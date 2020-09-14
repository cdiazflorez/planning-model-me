package com.mercadolibre.planning.model.me.clients.rest.outboundunit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpClient;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpRequest;
import com.mercadolibre.fbm.wms.outbound.commons.rest.RequestBodyHandler;
import com.mercadolibre.json.type.TypeReference;
import com.mercadolibre.planning.model.me.clients.rest.config.RestPool;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.UnitGroup;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitRequest;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.response.OutboundUnitSearchResponse;
import com.mercadolibre.restclient.RestClient;
import com.mercadolibre.restclient.exception.ParseException;
import com.newrelic.api.agent.Trace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;

@Slf4j
@Component
public class OutboundUnitClient extends HttpClient {

    private static final String SEARCH_GROUPS_URL = "/wms/outbound/groups/%s/search";
    public static final String CLIENT_ID = "9999";

    private final ObjectMapper objectMapper;

    protected OutboundUnitClient(final RestClient client, final ObjectMapper mapper) {
        super(client, RestPool.OUTBOUND_UNIT.name());
        this.objectMapper = mapper;
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
