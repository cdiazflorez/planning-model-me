package com.mercadolibre.planning.model.me.clients.rest.planningmodel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpClient;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpRequest;
import com.mercadolibre.fbm.wms.outbound.commons.rest.RequestBodyHandler;
import com.mercadolibre.json.type.TypeReference;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelForecastGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PostForecastResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.restclient.RestClient;
import com.mercadolibre.restclient.exception.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Set;

import static com.mercadolibre.planning.model.me.clients.rest.config.RestPool.PLANNING_MODEL_FORECAST;
import static java.lang.String.format;

@Component
public class PlanningModelForecastClient extends HttpClient
        implements PlanningModelForecastGateway {

    private static final String URL = "/planning/model/workflows/%s/forecasts";
    private final ObjectMapper objectMapper;

    public PlanningModelForecastClient(RestClient client, ObjectMapper objectMapper) {
        super(client, PLANNING_MODEL_FORECAST.name());
        this.objectMapper = objectMapper;
    }

    @Override
    public PostForecastResponse postForecast(final Workflow workflow, final Forecast forecastDto) {
        final HttpRequest request = HttpRequest.builder()
                .url(format(URL, workflow))
                .POST(requestSupplier(forecastDto))
                .acceptedHttpStatuses(Set.of(HttpStatus.OK, HttpStatus.CREATED))
                .build();

        return send(request, response -> response.getData(new TypeReference<>() {}));
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
