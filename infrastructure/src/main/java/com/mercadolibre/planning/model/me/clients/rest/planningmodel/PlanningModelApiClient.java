package com.mercadolibre.planning.model.me.clients.rest.planningmodel;

import static com.mercadolibre.planning.model.me.clients.rest.config.RestPool.PLANNING_MODEL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.PRODUCTIVITY;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpClient;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpRequest;
import com.mercadolibre.fbm.wms.outbound.commons.rest.RequestBodyHandler;
import com.mercadolibre.fbm.wms.outbound.commons.rest.exception.ClientException;
import com.mercadolibre.json.type.TypeReference;
import com.mercadolibre.planning.model.me.clients.rest.planningmodel.exception.ForecastNotFoundException;
import com.mercadolibre.planning.model.me.clients.rest.planningmodel.request.BacklogProjectionInAreasRequest;
import com.mercadolibre.planning.model.me.clients.rest.planningmodel.response.EntityResponse;
import com.mercadolibre.planning.model.me.clients.rest.planningmodel.response.ProductivityResponse;
import com.mercadolibre.planning.model.me.entities.sharedistribution.ShareDistribution;
import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.entity.EntityGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ConfigurationRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ConfigurationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.DeviationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ForecastMetadataRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.GetDeviationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.GetUnitsResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Metadata;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Productivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProductivityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SaveSimulationsRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SaveUnitsResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SearchTrajectoriesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SuggestedWave;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SuggestedWavesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.TrajectoriesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.BacklogProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.gateways.projection.ProjectionGateway;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogAreaDistribution;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogQuantityAtSla;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.ProjectedBacklogForAnAreaAndOperatingHour;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.DisableDeviationInput;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.SaveDeviationInput;
import com.mercadolibre.planning.model.me.usecases.sharedistribution.dtos.GetShareDistributionInput;
import com.mercadolibre.restclient.MeliRestClient;
import com.mercadolibre.restclient.exception.ParseException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PlanningModelApiClient extends HttpClient implements PlanningModelGateway, EntityGateway, ProjectionGateway {

  private static final String WORKFLOWS_URL = "/planning/model/workflows/%s";

  private static final String CONFIGURATION_URL = "/planning/model/configuration";

  private static final String SIMULATIONS_PREFIX_URL = "/simulations";

  private static final String PROJECTION_URL = "/planning/model/workflows/%s/projections/%s";

  private static final String DEVIATIONS_URL = "/deviations";

  private static final String WAREHOUSE_ID = "warehouse_id";

  private static final String DATE_FROM = "date_from";

  private static final String DATE_TO = "date_to";

  private static final String UNITS_DISTRIBUTION = "/entities/units_distribution";

  private static final String SAVE = "/save";

  private final ObjectMapper objectMapper;

  public PlanningModelApiClient(MeliRestClient client, ObjectMapper objectMapper) {
    super(client, PLANNING_MODEL.name());
    this.objectMapper = objectMapper;
  }

  @Override
  public List<MagnitudePhoto> getTrajectories(final TrajectoriesRequest trajectoriesRequest) {
    final HttpRequest request = HttpRequest.builder()
        .url(format(WORKFLOWS_URL + "/entities/%s",
                    trajectoriesRequest.getWorkflow().getName(),
                    trajectoriesRequest.getEntityType().getName()))
        .POST(requestSupplier(trajectoriesRequest))
        .queryParams(createEntityParams(trajectoriesRequest))
        .acceptedHttpStatuses(Set.of(OK))
        .build();

    return executeGetEntities(request);
  }

  @Override
  public List<Metadata> getForecastMetadata(final Workflow workflow,
                                            final ForecastMetadataRequest forecastMetadataRequest) {
    final HttpRequest request = HttpRequest.builder()
        .url(format(WORKFLOWS_URL, workflow) + "/metadata")
        .GET()
        .queryParams(createForecastMetadataParams(forecastMetadataRequest))
        .acceptedHttpStatuses(Set.of(OK))
        .build();

    return send(request, response -> response.getData(new TypeReference<>() {
    }));
  }

  protected Map<String, String> createForecastMetadataParams(
      final ForecastMetadataRequest request) {
    return getBaseParam(request.getWarehouseId(), request.getDateFrom(), request.getDateTo());
  }

  @Override
  public List<Productivity> getProductivity(final ProductivityRequest productivityRequest) {
    final HttpRequest request = HttpRequest.builder()
        .url(format(WORKFLOWS_URL + "/entities/productivity",
                    productivityRequest.getWorkflow().getName()))
        .POST(requestSupplier(productivityRequest))
        .queryParams(createEntityParams(productivityRequest))
        .acceptedHttpStatuses(Set.of(OK))
        .build();

    final List<ProductivityResponse> apiResponse = send(request, response ->
        response.getData(new TypeReference<>() {
        }));

    return apiResponse.stream().map(this::toProductivity).collect(toList());
  }

  @Override
  public List<MagnitudePhoto> getPerformedProcessing(final TrajectoriesRequest request) {
    final HttpRequest httpRequest = HttpRequest.builder()
        .url(format(WORKFLOWS_URL + "/entities/performed_processing",
                    request.getWorkflow().getName()))
        .GET()
        .queryParams(createEntityParams(request))
        .acceptedHttpStatuses(Set.of(OK))
        .build();

    return executeGetEntities(httpRequest);
  }

  @Override
  public Map<MagnitudeType, List<MagnitudePhoto>> searchTrajectories(
      final SearchTrajectoriesRequest request
  ) {
    final HttpRequest httpRequest = HttpRequest.builder()
        .url(format(WORKFLOWS_URL + "/entities/search", request.getWorkflow().getName()))
        .POST(requestSupplier(request))
        .acceptedHttpStatuses(Set.of(OK))
        .build();

    final Map<String, List<Object>> apiResponse = send(httpRequest, response ->
        response.getData(new TypeReference<>() {
        })
    );

    final Map<MagnitudeType, List<MagnitudePhoto>> response = new HashMap<>();
    apiResponse.forEach((key, value) -> {
      if (PRODUCTIVITY.getName().equals(key)) {
        response.put(PRODUCTIVITY, value.stream()
            .map(o -> toProductivity(objectMapper.convertValue(
                o, ProductivityResponse.class)))
            .collect(toList()));
      } else {
        response.put(MagnitudeType.from(key), value.stream()
            .map(o -> toEntity(objectMapper.convertValue(o, EntityResponse.class)))
            .collect(toList()));
      }
    });
    return response;
  }

  private List<ProjectionResult> callProjection(final ProjectionRequest projectionRequest,
                                                final String path) {
    final HttpRequest request = HttpRequest.builder()
        .url(format(PROJECTION_URL, projectionRequest.getWorkflow(), path))
        .POST(requestSupplier(projectionRequest))
        .acceptedHttpStatuses(Set.of(OK))
        .build();

    try {
      return send(request, response -> response.getData(new TypeReference<>() {
      }));
    } catch (ClientException ex) {
      if (forecastNotFound(ex)) {
        throw new ForecastNotFoundException();
      }
      throw ex;
    }
  }

  @Override
  public List<ProjectionResult> runProjection(final ProjectionRequest projectionRequest) {
    return callProjection(projectionRequest, "cpts");
  }

  @Override
  public List<ProjectionResult> runDeferralProjection(final ProjectionRequest projectionRequest) {
    return callProjection(projectionRequest, "cpts/delivery_promise");
  }

  @Override
  public List<ProjectionResult> runSimulation(final SimulationRequest simulationRequest) {
    final HttpRequest request = HttpRequest.builder()
        .url(format(WORKFLOWS_URL, simulationRequest.getWorkflow())
                 + SIMULATIONS_PREFIX_URL + "/run")
        .POST(requestSupplier(simulationRequest))
        .acceptedHttpStatuses(Set.of(OK))
        .build();

    return send(request, response -> response.getData(new TypeReference<>() {
    }));
  }

  @Override
  public List<ProjectionResult> saveSimulation(final SimulationRequest simulationRequest) {
    final HttpRequest request = HttpRequest.builder()
        .url(format(WORKFLOWS_URL, simulationRequest.getWorkflow())
                 + SIMULATIONS_PREFIX_URL + SAVE)
        .POST(requestSupplier(simulationRequest))
        .acceptedHttpStatuses(Set.of(OK))
        .build();

    return send(request, response -> response.getData(new TypeReference<>() {
    }));
  }

  @Override
  public Optional<ConfigurationResponse> getConfiguration(
      final ConfigurationRequest configurationRequest) {
    final Map<String, String> params = new LinkedHashMap<>();
    params.put("logistic_center_id", configurationRequest.getWarehouseId());
    params.put("key", configurationRequest.getKey());
    final HttpRequest request = HttpRequest.builder()
        .url(CONFIGURATION_URL)
        .GET()
        .queryParams(params)
        .acceptedHttpStatuses(Set.of(OK))
        .build();

    final ConfigurationResponse configurationResponse =
        send(request, response -> response.getData(new TypeReference<>() {
        }));

    return ofNullable(configurationResponse);
  }

  @Override
  public List<PlanningDistributionResponse> getPlanningDistribution(
      final PlanningDistributionRequest planningDistributionRequest) {
    final HttpRequest request = HttpRequest.builder()
        .url(format(WORKFLOWS_URL + "/planning_distributions",
                    planningDistributionRequest.getWorkflow().getName()))
        .GET()
        .queryParams(createPlanningDistributionParams(planningDistributionRequest))
        .acceptedHttpStatuses(Set.of(OK))
        .build();

    try {
      return send(request, response -> response.getData(new TypeReference<>() {
      }));
    } catch (ClientException ex) {
      if (forecastNotFound(ex)) {
        throw new ForecastNotFoundException();
      }

      throw ex;
    }
  }

  @Override
  public List<SuggestedWave> getSuggestedWaves(final SuggestedWavesRequest input) {
    final Map<String, String> params =
        getBaseParam(input.getWarehouseId(), input.getDateFrom(), input.getDateTo());
    params.put("backlog", input.getBacklog().toString());
    params.put("apply_deviation", valueOf(input.isApplyDeviation()));

    final HttpRequest request = HttpRequest.builder()
        .url(format(WORKFLOWS_URL + "/projections/suggested_waves", input.getWorkflow()))
        .GET()
        .queryParams(params)
        .acceptedHttpStatuses(Set.of(OK))
        .build();
    return send(request, response -> response.getData(new TypeReference<>() {
    }));
  }

  @Override
  public List<BacklogProjectionResponse> getBacklogProjection(
      final BacklogProjectionRequest request) {

    final HttpRequest httpRequest = HttpRequest.builder()
        .url(format(PROJECTION_URL, request.getWorkflow(), "backlogs"))
        .POST(requestSupplier(request))
        .acceptedHttpStatuses(Set.of(OK))
        .build();

    return send(httpRequest, response -> response.getData(new TypeReference<>() {
    }));
  }

  @Override
  public List<ProjectedBacklogForAnAreaAndOperatingHour> projectBacklogInAreas(final Instant dateFrom,
                                                                               final Instant dateTo,
                                                                               final Workflow workflow,
                                                                               final List<ProcessName> processes,
                                                                               final List<BacklogQuantityAtSla> backlog,
                                                                               final List<PlanningDistributionResponse> plannedUnits,
                                                                               final List<MagnitudePhoto> throughput,
                                                                               final List<BacklogAreaDistribution> backlogDistribution) {

    final var request = new BacklogProjectionInAreasRequest(
        dateFrom,
        dateTo,
        processes,
        throughput,
        plannedUnits,
        backlog,
        backlogDistribution
    );

    final HttpRequest httpRequest = HttpRequest.builder()
        .url(format(PROJECTION_URL, workflow, "backlogs/grouped/area"))
        .POST(requestSupplier(request))
        .acceptedHttpStatuses(Set.of(OK))
        .build();

    return send(httpRequest, response -> response.getData(new TypeReference<>() {
    }));
  }

  @Override
  public void deferralSaveSimulation(SaveSimulationsRequest saveSimulationsRequest) {
    final HttpRequest request = HttpRequest.builder()
        .url(format(WORKFLOWS_URL, saveSimulationsRequest.getWorkflow())
                 + SIMULATIONS_PREFIX_URL + "/deferral/save")
        .POST(requestSupplier(saveSimulationsRequest))
        .acceptedHttpStatuses(Set.of(OK))
        .build();

    send(request, response -> response.getData(new TypeReference<>() {
    }));
  }

  @Override
  public GetDeviationResponse getDeviation(final Workflow workflow,
                                           final String warehouseId,
                                           final ZonedDateTime date) {
    final Map<String, String> params = new HashMap<>();
    params.put(WAREHOUSE_ID, warehouseId);
    params.put("date", date.withFixedOffsetZone().toString());

    final HttpRequest request = HttpRequest.builder()
        .url(format(WORKFLOWS_URL, workflow)
                 + DEVIATIONS_URL)
        .GET()
        .queryParams(params)
        .acceptedHttpStatuses(Set.of(OK, CREATED))
        .build();

    return send(request, response -> response.getData(new TypeReference<>() {
    }));
  }

  @Override
  public SaveUnitsResponse saveShareDistribution(final List<ShareDistribution> shareDistributionList, final Workflow workflow) {
    final HttpRequest request = HttpRequest.builder()
        .url(format(WORKFLOWS_URL, workflow) + UNITS_DISTRIBUTION)
        .POST(requestSupplier(shareDistributionList))
        .acceptedHttpStatuses(Set.of(OK, CREATED))
        .build();

    return send(request, response -> response.getData(new TypeReference<>() {
    }));
  }

  @Override
  public List<GetUnitsResponse> getShareDistribution(final GetShareDistributionInput getShareDistributionInput, final Workflow workflow) {

    final Map<String, String> params = new HashMap<>();
    params.put(WAREHOUSE_ID, getShareDistributionInput.getWareHouseId());
    params.put(DATE_FROM, getShareDistributionInput.getDateFrom().toString());
    params.put(DATE_TO, getShareDistributionInput.getDateTo().toString());

    final HttpRequest request = HttpRequest.builder()
        .url(format(WORKFLOWS_URL, workflow) + UNITS_DISTRIBUTION)
        .GET()
        .queryParams(params)
        .acceptedHttpStatuses(Set.of(OK))
        .build();
    return send(request, response -> response.getData(new TypeReference<>() {
    }));
  }

  @Override
  public DeviationResponse saveDeviation(final SaveDeviationInput saveDeviationInput) {
    final HttpRequest request = HttpRequest.builder()
        .url(format(WORKFLOWS_URL, saveDeviationInput.getWorkflow())
                 + DEVIATIONS_URL + SAVE)
        .POST(requestSupplier(saveDeviationInput))
        .acceptedHttpStatuses(Set.of(OK, CREATED))
        .build();

    return send(request, response -> response.getData(new TypeReference<>() {
    }));
  }

  @Override
  public DeviationResponse disableDeviation(DisableDeviationInput disableDeviationInput) {
    final HttpRequest request = HttpRequest.builder()
        .url(format(WORKFLOWS_URL, disableDeviationInput.getWorkflow())
                 + DEVIATIONS_URL + "/disable")
        .POST(requestSupplier(disableDeviationInput))
        .acceptedHttpStatuses(Set.of(OK, CREATED))
        .build();

    return send(request, response -> response.getData(new TypeReference<>() {
    }));
  }

  protected Map<String, String> createEntityParams(final TrajectoriesRequest request) {
    final Map<String, String> params = getBaseParam(request.getWarehouseId(),
                                                    request.getDateFrom(),
                                                    request.getDateTo());
    params.put("process_name", getEnumNamesAsString(request.getProcessName()));
    if (request.getProcessingType() != null) {
      params.put("processing_type", getEnumNamesAsString(request.getProcessingType()));
    }
    return params;
  }

  private Map<String, String> getBaseParam(String warehouseId,
                                           ZonedDateTime dateFrom,
                                           ZonedDateTime dateTo) {
    final Map<String, String> params = new LinkedHashMap<>();
    params.put(WAREHOUSE_ID, warehouseId);
    params.put(DATE_FROM, dateFrom.format(ISO_OFFSET_DATE_TIME));
    params.put(DATE_TO, dateTo.format(ISO_OFFSET_DATE_TIME));
    return params;
  }

  private Map<String, String> createPlanningDistributionParams(
      final PlanningDistributionRequest request) {
    final Map<String, String> params = new LinkedHashMap<>();
    addParameter(request.getWarehouseId(),
                 () -> params.put(WAREHOUSE_ID,
                                  request.getWarehouseId()));
    addParameter(request.getDateInFrom(),
                 () -> params.put("date_in_from",
                                  request.getDateInFrom().format(ISO_OFFSET_DATE_TIME)));
    addParameter(request.getDateInTo(),
                 () -> params.put("date_in_to",
                                  request.getDateInTo().format(ISO_OFFSET_DATE_TIME)));
    addParameter(request.getDateOutFrom(),
                 () -> params.put("date_out_from",
                                  request.getDateOutFrom().format(ISO_OFFSET_DATE_TIME)));
    addParameter(request.getDateOutTo(),
                 () -> params.put("date_out_to",
                                  request.getDateOutTo().format(ISO_OFFSET_DATE_TIME)));
    return params;
  }

  private void addParameter(Object parameter,
                            Runnable valueCallable) {
    ofNullable(parameter).ifPresent(t -> valueCallable.run());
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

  private List<MagnitudePhoto> executeGetEntities(HttpRequest request) {
    try {
      final List<EntityResponse> apiResponse = send(request, response ->
          response.getData(new TypeReference<>() {
          }));

      return apiResponse.stream().map(this::toEntity).collect(toList());
    } catch (ClientException ex) {
      if (forecastNotFound(ex)) {
        throw new ForecastNotFoundException();
      } else {
        throw ex;
      }
    }
  }

  private MagnitudePhoto toEntity(final EntityResponse response) {
    return MagnitudePhoto.builder()
        .date(ZonedDateTime.parse(response.getDate(), ISO_OFFSET_DATE_TIME))
        .processName(ProcessName.from(response.getProcessName()))
        .workflow(Workflow.from(response.getWorkflow()).orElseThrow())
        .value(response.getValue())
        .source(Source.from(response.getSource()))
        .metricUnit(MetricUnit.from(response.getMetricUnit()))
        .build();
  }

  private Productivity toProductivity(final ProductivityResponse response) {
    return Productivity.builder()
        .date(ZonedDateTime.parse(response.getDate(), ISO_OFFSET_DATE_TIME))
        .processName(ProcessName.from(response.getProcessName()))
        .workflow(Workflow.from(response.getWorkflow()).orElseThrow())
        .value(response.getValue())
        .source(Source.from(response.getSource()))
        .metricUnit(MetricUnit.from(response.getMetricUnit()))
        .abilityLevel(response.getAbilityLevel())
        .build();
  }

  private boolean forecastNotFound(ClientException ex) {
    return ex.getResponseStatus() == NOT_FOUND.value()
        && ex.getResponseBody().contains("forecast_not_found");
  }
}
