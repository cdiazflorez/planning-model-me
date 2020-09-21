package com.mercadolibre.planning.model.me.clients.rest.planningmodel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpClient;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpRequest;
import com.mercadolibre.fbm.wms.outbound.commons.rest.RequestBodyHandler;
import com.mercadolibre.json.type.TypeReference;
import com.mercadolibre.planning.model.me.clients.rest.planningmodel.response.EntityResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.restclient.RestClient;
import com.mercadolibre.restclient.exception.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.mercadolibre.planning.model.me.clients.rest.config.RestPool.PLANNING_MODEL;
import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.stream.Collectors.toList;

@Component
public class PlanningModelApiClient extends HttpClient implements PlanningModelGateway {

    private static final String URL = "/planning/model/workflows/%s";
    private final ObjectMapper objectMapper;


    public PlanningModelApiClient(RestClient client, ObjectMapper objectMapper) {
        super(client, PLANNING_MODEL.name());
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Entity> getEntities(final EntityRequest entityRequest) {

        final HttpRequest request = HttpRequest.builder()
                .url(format(URL + "/entities/%s",
                        entityRequest.getWorkflow().getName(),
                        entityRequest.getEntityType().getName()))
                .GET()
                .queryParams(createEntityParams(entityRequest))
                .acceptedHttpStatuses(Set.of(HttpStatus.OK))
                .build();
        final List<EntityResponse> apiResponse = send(request, response ->
                response.getData(new TypeReference<>() {}));

        return apiResponse.stream().map(this::toEntity).collect(toList());
    }

    private Entity toEntity(final EntityResponse response) {
        return Entity.builder()
                .date(ZonedDateTime.parse(response.getDate(), ISO_OFFSET_DATE_TIME))
                .processName(ProcessName.from(response.getProcessName()))
                .workflow(Workflow.from(response.getWorkflow()))
                .value(response.getValue())
                .source(Source.from(response.getSource()))
                .metricUnit(MetricUnit.from(response.getMetricUnit()))
                .build();
    }

    private Map<String, String> createEntityParams(final EntityRequest request) {
        final Map<String, String> params = new LinkedHashMap<>();
        params.put("warehouse_id", request.getWarehouseId());
        params.put("date_from", request.getDateFrom().format(ISO_OFFSET_DATE_TIME));
        params.put("date_to", request.getDateTo().format(ISO_OFFSET_DATE_TIME));

        if (request.getSource() != null) {
            params.put("source", request.getSource().getName());
        }
        return params;
    }

    @Override
    public void postForecast(final Workflow workflow, final Forecast forecastDto) {
        final HttpRequest request = HttpRequest.builder()
                .url(format(URL, workflow) + "/forecasts")
                .POST(requestSupplier(forecastDto))
                .acceptedHttpStatuses(Set.of(HttpStatus.OK, HttpStatus.CREATED))
                .build();

        send(request, response -> response.getData(new TypeReference<>() {}));
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
