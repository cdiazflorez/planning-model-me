package com.mercadolibre.planning.model.me.gateways.planningmodel;

import com.mercadolibre.planning.model.me.entities.projection.ProjectionResult;
import com.mercadolibre.planning.model.me.entities.simulation.SimulationResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ConfigurationRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ConfigurationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ForecastResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;

import java.util.List;

public interface PlanningModelGateway {

    List<Entity> getEntities(final EntityRequest request);

    ForecastResponse postForecast(final Workflow workflow, final Forecast forecastDto);

    List<ProjectionResult> runProjection(final ProjectionRequest request);

    List<SimulationResult> runSimulation(final SimulationRequest request);

    ConfigurationResponse getConfiguration(final ConfigurationRequest request);

    List<PlanningDistributionResponse> getPlanningDistribution(
            final PlanningDistributionRequest request);
}
