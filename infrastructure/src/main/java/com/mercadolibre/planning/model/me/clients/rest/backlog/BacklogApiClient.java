package com.mercadolibre.planning.model.me.clients.rest.backlog;

import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpClient;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpRequest;
import com.mercadolibre.json.type.TypeReference;
import com.mercadolibre.planning.model.me.clients.rest.config.RestPool;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.restclient.MeliRestClient;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static org.springframework.http.HttpStatus.OK;

@Component
public class BacklogApiClient extends HttpClient implements BacklogApiGateway {
    private static final String BACKLOG_URL = "/backlogs/logistic_centers/%s/backlogs";

    public BacklogApiClient(final MeliRestClient client) {
        super(client, RestPool.BACKLOG.name());
    }

    public List<Consolidation> getBacklog(BacklogRequest request) {
        final HttpRequest httpRequest = HttpRequest.builder()
                .url(format(BACKLOG_URL, request.getWarehouseId()))
                .GET()
                .queryParams(getQueryParams(request))
                .acceptedHttpStatuses(Set.of(OK))
                .build();

        return send(httpRequest, response ->
                response.getData(new TypeReference<>() {
                })
        );
    }

    private Map<String, String> getQueryParams(BacklogRequest request) {
        Map<String, String> params = new HashMap<>();
        addAsQueryParam(params, "requestDate", request.getRequestDate());
        addAsQueryParam(params, "workflows", request.getWorkflows());
        addAsQueryParam(params, "processes", request.getProcesses());
        addAsQueryParam(params, "group_by", request.getGroupingFields());
        addAsQueryParam(params, "date_from", request.getDateFrom());
        addAsQueryParam(params, "date_to", request.getDateTo());
        addAsQueryParam(params, "sla_from", request.getSlaFrom());
        addAsQueryParam(params, "sla_to", request.getSlaTo());

        return params;
    }

    private void addAsQueryParam(Map<String, String> map, String key, List<String> value) {
        if (value != null) {
            map.put(key, String.join(",", value));
        }
    }

    private void addAsQueryParam(Map<String, String> map, String key, Instant value) {
        if (value != null) {
            map.put(key, ISO_INSTANT.format(value));
        }
    }
}
