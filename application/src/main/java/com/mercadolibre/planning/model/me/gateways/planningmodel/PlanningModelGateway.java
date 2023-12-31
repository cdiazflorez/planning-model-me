package com.mercadolibre.planning.model.me.gateways.planningmodel;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ConfigurationRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ConfigurationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.CycleTimeRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ForecastMetadataRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.GetDeviationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Metadata;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Productivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProductivityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SearchTrajectoriesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SlaProperties;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.TrajectoriesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.BacklogProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @deprecated use any of {@link com.mercadolibre.planning.model.me.gateways.entity.EntityGateway},
 *     {@link  com.mercadolibre.planning.model.me.gateways.projection.ProjectionGateway},
 *     {@link DeviationGateway} or create a new Gateway if needed.
 */
@Deprecated
public interface PlanningModelGateway {

  List<MagnitudePhoto> getTrajectories(TrajectoriesRequest request);

  List<Metadata> getForecastMetadata(Workflow workflow, ForecastMetadataRequest request);

  List<Productivity> getProductivity(ProductivityRequest request);

  List<MagnitudePhoto> getPerformedProcessing(TrajectoriesRequest request);

  Map<MagnitudeType, List<MagnitudePhoto>> searchTrajectories(SearchTrajectoriesRequest request);

  List<ProjectionResult> runProjection(ProjectionRequest request);

  List<ProjectionResult> runDeferralProjection(ProjectionRequest request);

  List<ProjectionResult> runSimulation(SimulationRequest request);

  List<ProjectionResult> saveSimulation(SimulationRequest request);

  List<BacklogProjectionResponse> getBacklogProjection(BacklogProjectionRequest request);

  Optional<ConfigurationResponse> getConfiguration(ConfigurationRequest request);

  List<PlanningDistributionResponse> getPlanningDistribution(PlanningDistributionRequest request);

  GetDeviationResponse getDeviation(Workflow workflow, String warehouseId, ZonedDateTime date);

  Map<Workflow, Map<Instant, SlaProperties>> getCycleTime(String logisticCenterId, CycleTimeRequest cycleTimeRequest);
}
