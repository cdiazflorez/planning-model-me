package com.mercadolibre.planning.model.me.clients.rest.planningmodel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpClient;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpRequest;
import com.mercadolibre.fbm.wms.outbound.commons.rest.RequestBodyHandler;
import com.mercadolibre.json.type.TypeReference;
import com.mercadolibre.planning.model.me.clients.rest.planningmodel.response.EntityResponse;
import com.mercadolibre.planning.model.me.entities.projection.ProjectionResult;
import com.mercadolibre.planning.model.me.entities.simulation.SimulationResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ConfigurationRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ConfigurationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ForecastResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationRequest;
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
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

@Component
public class PlanningModelApiClient extends HttpClient implements PlanningModelGateway {

    private static final String WORKFLOWS_URL = "/planning/model/workflows/%s";
    private static final String CONFIGURATION_URL = "/planning/model/configuration";
    private static final String SIMULATIONS_PREFIX_URL = "/simulations";
    private final ObjectMapper objectMapper;


    public PlanningModelApiClient(RestClient client, ObjectMapper objectMapper) {
        super(client, PLANNING_MODEL.name());
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Entity> getEntities(final EntityRequest entityRequest) {
        final HttpRequest request = HttpRequest.builder()
                .url(format(WORKFLOWS_URL + "/entities/%s",
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
                .workflow(Workflow.from(response.getWorkflow()).orElseThrow())
                .value(response.getValue())
                .source(Source.from(response.getSource()))
                .metricUnit(MetricUnit.from(response.getMetricUnit()))
                .build();
    }

    protected Map<String, String> createEntityParams(final EntityRequest request) {
        final Map<String, String> params = new LinkedHashMap<>();
        params.put("warehouse_id", request.getWarehouseId());
        params.put("date_from", request.getDateFrom().format(ISO_OFFSET_DATE_TIME));
        params.put("date_to", request.getDateTo().format(ISO_OFFSET_DATE_TIME));
        params.put("process_name", getEnumNamesAsString(request.getProcessName()));
        if (request.getProcessingType() != null) {
            params.put("processing_type", getEnumNamesAsString(request.getProcessingType()));
        }
        return params;
    }

    @Override
    public ForecastResponse postForecast(final Workflow workflow, final Forecast forecastDto) {
        final HttpRequest request = HttpRequest.builder()
                .url(format(WORKFLOWS_URL, workflow) + "/forecasts")
                .POST(requestSupplier(forecastDto))
                .acceptedHttpStatuses(Set.of(HttpStatus.OK, HttpStatus.CREATED))
                .build();

        return send(request, response -> response.getData(new TypeReference<>() {}));
    }

    @Override
    public List<ProjectionResult> runProjection(final ProjectionRequest projectionRequest) {
        final HttpRequest request = HttpRequest.builder()
                .url(format(WORKFLOWS_URL, projectionRequest.getWorkflow()) + "/projections")
                .POST(requestSupplier(projectionRequest))
                .acceptedHttpStatuses(Set.of(HttpStatus.OK))
                .build();

        return send(request, response -> response.getData(new TypeReference<>() {}));
    }

    @Override
    public List<SimulationResult> runSimulation(final SimulationRequest simulationRequest) {
        final HttpRequest request = HttpRequest.builder()
                .url(format(WORKFLOWS_URL, simulationRequest.getWorkflow())
                        + SIMULATIONS_PREFIX_URL + "/run")
                .POST(requestSupplier(simulationRequest))
                .acceptedHttpStatuses(Set.of(HttpStatus.OK))
                .build();

        return send(request, response -> response.getData(new TypeReference<>() {}));
    }

    @Override
    public List<SimulationResult> saveSimulation(final SimulationRequest simulationRequest) {
        final HttpRequest request = HttpRequest.builder()
                .url(format(WORKFLOWS_URL, simulationRequest.getWorkflow())
                        + SIMULATIONS_PREFIX_URL + "/save")
                .POST(requestSupplier(simulationRequest))
                .acceptedHttpStatuses(Set.of(HttpStatus.OK))
                .build();

        return send(request, response -> response.getData(new TypeReference<>() {}));
    }

    @Override
    public ConfigurationResponse getConfiguration(final ConfigurationRequest configurationRequest) {
        final Map<String, String> params = new LinkedHashMap<>();
        params.put("warehouse_id", configurationRequest.getWarehouseId());
        params.put("key", configurationRequest.getKey());

        final HttpRequest request = HttpRequest.builder()
                .url(CONFIGURATION_URL)
                .GET()
                .queryParams(params)
                .acceptedHttpStatuses(Set.of(HttpStatus.OK))
                .build();

        return send(request, response -> response.getData(new TypeReference<>() {}));
    }

    @Override
    public List<PlanningDistributionResponse> getPlanningDistribution(
            final PlanningDistributionRequest planningDistributionRequest) {
        final HttpRequest request = HttpRequest.builder()
                .url(format(WORKFLOWS_URL + "/planning_distributions",
                        planningDistributionRequest.getWorkflow().getName()))
                .GET()
                .queryParams(createPlanningDistributionParams(planningDistributionRequest))
                .acceptedHttpStatuses(Set.of(HttpStatus.OK))
                .build();

        return send(request, response -> response.getData(new TypeReference<>() {}));
    }

    private Map<String, String> createPlanningDistributionParams(
            final PlanningDistributionRequest request) {
        final Map<String, String> params = new LinkedHashMap<>();
        params.put("warehouse_id", request.getWarehouseId());
        params.put("date_in_to", request.getDateInTo().format(ISO_OFFSET_DATE_TIME));
        params.put("date_out_from", request.getDateOutFrom().format(ISO_OFFSET_DATE_TIME));
        params.put("date_out_to", request.getDateOutTo().format(ISO_OFFSET_DATE_TIME));

        return params;
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

    private String getEnumNamesAsString(final List<? extends Enum> namedEnums) {
        return namedEnums.stream().map(Enum::name).map(String::toLowerCase)
                .collect(joining(","));
    }
}
