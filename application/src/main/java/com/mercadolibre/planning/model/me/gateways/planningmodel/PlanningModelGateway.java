package com.mercadolibre.planning.model.me.gateways.planningmodel;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ConfigurationRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ConfigurationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.DeviationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ForecastMetadataRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.GetDeviationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Metadata;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Productivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProductivityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SearchEntitiesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SuggestedWave;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SuggestedWavesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.BacklogProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.DisableDeviationInput;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.SaveDeviationInput;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PlanningModelGateway {

    List<Entity> getEntities(final EntityRequest request);

    List<Metadata> getForecastMetadata(final Workflow workflow,
                                       final ForecastMetadataRequest request);

    List<Productivity> getProductivity(final ProductivityRequest request);

    Map<EntityType, List<Entity>> searchEntities(final SearchEntitiesRequest request);

    List<ProjectionResult> runProjection(final ProjectionRequest request);

    List<ProjectionResult> runSimulation(final SimulationRequest request);

    List<ProjectionResult> saveSimulation(final SimulationRequest request);

    List<BacklogProjectionResponse> getBacklogProjection(final BacklogProjectionRequest request);

    Optional<ConfigurationResponse> getConfiguration(final ConfigurationRequest request);

    List<PlanningDistributionResponse> getPlanningDistribution(
            final PlanningDistributionRequest request);

    List<SuggestedWave> getSuggestedWaves(final SuggestedWavesRequest suggestedWavesRequest);

    DeviationResponse saveDeviation(final SaveDeviationInput saveDeviationInput);

    DeviationResponse disableDeviation(final DisableDeviationInput saveDeviationInput);

    GetDeviationResponse getDeviation(final Workflow workflow,
                                      final String warehouseId);

}
