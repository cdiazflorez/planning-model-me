package com.mercadolibre.planning.model.me.gateways.planningmodel;

import com.mercadolibre.planning.model.me.entities.projection.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ConfigurationRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ConfigurationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ForecastMetadataRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ForecastResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Metadata;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Productivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProductivityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;

import java.util.List;
import java.util.Optional;

public interface PlanningModelGateway {

    List<Entity> getEntities(final EntityRequest request);

    List<Metadata> getForecastMetadata(final Workflow workflow,
                                       final ForecastMetadataRequest request);

    List<Productivity> getProductivity(final ProductivityRequest request);

    ForecastResponse postForecast(final Workflow workflow, final Forecast forecastDto);

    List<ProjectionResult> runProjection(final ProjectionRequest request);

    List<ProjectionResult> runSimulation(final SimulationRequest request);

    List<ProjectionResult> saveSimulation(final SimulationRequest request);

    Optional<ConfigurationResponse> getConfiguration(final ConfigurationRequest request);

    List<PlanningDistributionResponse> getPlanningDistribution(
            final PlanningDistributionRequest request);
}
