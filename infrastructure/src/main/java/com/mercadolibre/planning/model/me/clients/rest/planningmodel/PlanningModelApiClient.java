package com.mercadolibre.planning.model.me.clients.rest.planningmodel;

import static com.mercadolibre.planning.model.me.clients.rest.config.RestPool.PLANNING_MODEL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.INBOUND_TRANSFER;
import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
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
import com.mercadolibre.planning.model.me.clients.rest.planningmodel.request.DeferralProjectionStatusRequest;
import com.mercadolibre.planning.model.me.clients.rest.planningmodel.response.EntityResponse;
import com.mercadolibre.planning.model.me.clients.rest.planningmodel.response.ProductivityResponse;
import com.mercadolibre.planning.model.me.entities.sharedistribution.ShareDistribution;
import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.entity.EntityGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.DeviationGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ConfigurationRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ConfigurationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.CycleTimeRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Deviation;
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
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Productivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProductivityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SaveDeviationRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SaveSimulationsRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SaveUnitsResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SearchTrajectoriesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Simulation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SlaProperties;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.TrajectoriesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.BacklogProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.gateways.projection.ProjectionGateway;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogAreaDistribution;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogQuantity;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogQuantityAtSla;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.ProjectedBacklogForAnAreaAndOperatingHour;
import com.mercadolibre.planning.model.me.gateways.projection.deferral.DeferralProjectionStatus;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.DisableDeviationInput;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.SaveDeviationInput;
import com.mercadolibre.planning.model.me.usecases.sharedistribution.dtos.GetShareDistributionInput;
import com.mercadolibre.restclient.MeliRestClient;
import com.mercadolibre.restclient.exception.ParseException;
import com.newrelic.api.agent.Trace;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PlanningModelApiClient extends HttpClient implements PlanningModelGateway, EntityGateway, ProjectionGateway, DeviationGateway {

  private static final String WORKFLOWS_URL = "/planning/model/workflows/%s";

  private static final String BASE_DEVIATIONS_URL = "/planning/model/workflows/%s/deviations/%s";

  private static final String CONFIGURATION_URL = "/planning/model/configuration";

  private static final String SIMULATIONS_PREFIX_URL = "/simulations";

  private static final String PROJECTION_URL = "/planning/model/workflows/%s/projections/%s";

  private static final String DEVIATIONS_URL = "/deviations";

  private static final String ACTIVE_DEVIATIONS_URL = "/deviations/active";

  private static final String WAREHOUSE_ID = "warehouse_id";

  private static final String LOGISTIC_CENTER_ID = "logistic_center_id";

  private static final String DATE_FROM = "date_from";

  private static final String DATE_TO = "date_to";

  private static final String UNITS_DISTRIBUTION = "/entities/units_distribution";

  private static final String SAVE = "save/";

  private static final String DELIMITER = ",";

  //TODO: Remove this when using traffic routes
  private static final Workflow IGNORABLE_WORKFLOW = INBOUND;

  private static final Map<Workflow, String> PARAM_NAME_BY_WORKFLOW = Map.of(
      FBM_WMS_INBOUND, FBM_WMS_INBOUND.getName(),
      FBM_WMS_OUTBOUND, FBM_WMS_OUTBOUND.getName(),
      INBOUND, FBM_WMS_INBOUND.getName(),
      INBOUND_TRANSFER, FBM_WMS_INBOUND.getName()
  );

  private final ObjectMapper objectMapper;

  public PlanningModelApiClient(MeliRestClient client, ObjectMapper objectMapper) {
    super(client, PLANNING_MODEL.name());
    this.objectMapper = objectMapper;
  }

  @Trace
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

  @Trace
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

  @Trace
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

  @Trace
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

  @Trace
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

  @Trace
  @Override
  public List<ProjectionResult> runProjection(final ProjectionRequest projectionRequest) {
    return callProjection(projectionRequest, "cpts");
  }

  @Trace
  @Override
  public List<ProjectionResult> runDeferralProjection(final ProjectionRequest projectionRequest) {
    return callProjection(projectionRequest, "cpts/delivery_promise");
  }

  @Trace
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

  @Trace
  @Override
  public List<ProjectionResult> saveSimulation(final SimulationRequest simulationRequest) {
    final HttpRequest request = HttpRequest.builder()
        .url(format(WORKFLOWS_URL, simulationRequest.getWorkflow())
            + SIMULATIONS_PREFIX_URL + "/save")
        .POST(requestSupplier(simulationRequest))
        .acceptedHttpStatuses(Set.of(OK))
        .build();

    return send(request, response -> response.getData(new TypeReference<>() {
    }));
  }

  @Trace
  @Override
  public Optional<ConfigurationResponse> getConfiguration(
      final ConfigurationRequest configurationRequest) {
    final Map<String, String> params = new LinkedHashMap<>();
    params.put(LOGISTIC_CENTER_ID, configurationRequest.getWarehouseId());
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

  @Trace
  @Override
  public List<PlanningDistributionResponse> getPlanningDistribution(final PlanningDistributionRequest planningDistributionRequest) {
    final HttpRequest request = HttpRequest.builder()
        .url(format(WORKFLOWS_URL + "/planning_distributions", PARAM_NAME_BY_WORKFLOW.get(planningDistributionRequest.getWorkflow())))
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

  @Trace
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

  @Trace
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
        dateFrom.truncatedTo(ChronoUnit.HOURS),
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

  @Trace
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

  @Trace
  @Override
  public List<DeferralProjectionStatus> getDeferralProjectionStatus(
      Instant dateFrom,
      Instant dateTo,
      Workflow workflow,
      List<ProcessName> processes,
      List<BacklogQuantity> backlogs,
      String wareHouseId,
      String timeZone,
      boolean applyDeviation,
      List<Simulation> simulations) {

    final var deferralProjectionStatusRequest = new DeferralProjectionStatusRequest(
        dateFrom,
        dateTo,
        processes,
        backlogs,
        wareHouseId,
        timeZone,
        applyDeviation,
        simulations);


    final HttpRequest request = HttpRequest.builder()
        .url(format(PROJECTION_URL, workflow, "cpts/deferral_time"))
        .POST(requestSupplier(deferralProjectionStatusRequest))
        .acceptedHttpStatuses(Set.of(OK))
        .build();

    return send(request, response -> response.getData(new TypeReference<>() {
    }));
  }

  @Trace
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
        .acceptedHttpStatuses(Set.of(OK))
        .build();

    return send(request, response -> response.getData(new TypeReference<>() {
    }));
  }

  @Trace
  @Override
  public List<Deviation> getActiveDeviations(final Set<Workflow> workflows,
                                             final String warehouseId,
                                             final Instant date) {
    final Map<String, String> params = Map.of(
        WAREHOUSE_ID, warehouseId,
        "date", date.toString(),
        "workflows", workflows.stream().map(Workflow::getName).collect(joining(DELIMITER))
    );
    final HttpRequest request = HttpRequest.builder()
        //TODO delete this when traffic route is active.
        .url(format(WORKFLOWS_URL, FBM_WMS_INBOUND) + ACTIVE_DEVIATIONS_URL)
        .GET()
        .queryParams(params)
        .acceptedHttpStatuses(Set.of(OK))
        .build();

    return send(request, response -> response.getData(new TypeReference<>() {
    }));
  }

  @Trace
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

  @Trace
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

  @Trace
  @Override
  public void save(final List<SaveDeviationInput> deviations) {
    final List<SaveDeviationRequest> deviationRequests = deviations.stream()
        .map(SaveDeviationInput::toSaveDeviationApiRequest)
        .collect(Collectors.toList());

    final HttpRequest request = HttpRequest.builder()
        .url(format(BASE_DEVIATIONS_URL, IGNORABLE_WORKFLOW, "save/all"))
        .POST(requestSupplier(deviationRequests))
        .acceptedHttpStatuses(Set.of(OK, CREATED, NO_CONTENT))
        .build();
    send(request, response -> response.getData(new TypeReference<>() {
    }));
  }

  @Trace
  @Override
  public DeviationResponse disableDeviation(final String logisticCenterId, final List<DisableDeviationInput> disableDeviationInput) {
    final Map<String, String> params = Map.of(
        LOGISTIC_CENTER_ID, logisticCenterId
    );

    final HttpRequest request = HttpRequest.builder()
        .url(format(BASE_DEVIATIONS_URL, IGNORABLE_WORKFLOW, "disable/all"))
        .POST(requestSupplier(disableDeviationInput))
        .queryParams(params)
        .acceptedHttpStatuses(Set.of(OK, CREATED, NO_CONTENT))
        .build();

    return send(request, response -> response.getData(new TypeReference<>() {
    }));
  }

  @Trace
  @Override
  public Map<Workflow, Map<Instant, SlaProperties>> getCycleTime(final String logisticCenterId, final CycleTimeRequest cycleTimeRequest) {
    final HttpRequest request = HttpRequest.builder()
        .url(format(CONFIGURATION_URL + "/logistic_center_id/%s/cycle_time/search", logisticCenterId))
        .POST(requestSupplier(cycleTimeRequest))
        .acceptedHttpStatuses(Set.of(OK))
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

  private Map<String, String> createPlanningDistributionParams(final PlanningDistributionRequest request) {
    final Map<String, String> params = new HashMap<>();
    addParameter(params, WAREHOUSE_ID, request.getWarehouseId());
    addParameter(params, "date_in_from", request.getDateInFrom());
    addParameter(params, "date_in_to", request.getDateInTo());
    addParameter(params, "date_out_from", request.getDateOutFrom());
    addParameter(params, "date_out_to", request.getDateOutTo());
    addParameter(params, "apply_deviation", Boolean.toString(request.isApplyDeviation()));
    addParameter(params, "view_date", request.getDateOutTo());
    addParameter(params, "workflow", request.getWorkflow().getName());

    return params;
  }

  private void addParameter(final Map<String, String> parameters, final String name, final ZonedDateTime value) {
    if (name != null && value != null) {
      parameters.put(name, value.format(ISO_OFFSET_DATE_TIME));
    }
  }

  private void addParameter(final Map<String, String> parameters, final String name, final String value) {
    if (name != null && value != null) {
      parameters.put(name, value);
    }
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
        .collect(joining(DELIMITER));
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
        .type(ProcessingType.from(response.getType()))
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
