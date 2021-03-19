package com.mercadolibre.planning.model.me.metric;

import com.mercadolibre.planning.model.me.controller.deviation.request.DeviationRequest;
import com.mercadolibre.planning.model.me.controller.simulation.request.EntityRequest;
import com.mercadolibre.planning.model.me.controller.simulation.request.RunSimulationRequest;
import com.mercadolibre.planning.model.me.controller.simulation.request.SaveSimulationRequest;
import com.mercadolibre.planning.model.me.controller.simulation.request.SimulationRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static com.mercadolibre.metrics.MetricCollector.Tags;
import static com.mercadolibre.planning.model.me.config.EnvironmentUtil.getScope;


@Component
@Slf4j
@AllArgsConstructor
public class DatadogMetricService {

    private final DatadogWrapper datadog;

    public void trackRunSimulation(final RunSimulationRequest request) {

        final Tags tags = createSimulationTags(request.getWarehouseId(), request.getSimulations());

        try {
            datadog.incrementCounter("application.planning.model.simulation.run", tags);
        } catch (Exception e) {
            log.warn("[warehouse_id:{}] " + "Couldn't track run simulation metric",
                    request.getWarehouseId(),
                    e);
        }
    }

    public void trackSaveSimulation(final SaveSimulationRequest request) {

        final Tags tags = createSimulationTags(request.getWarehouseId(), request.getSimulations());

        try {
            datadog.incrementCounter("application.planning.model.simulation.save", tags);
        } catch (Exception e) {
            log.warn("[warehouse_id:{}] " + "Couldn't track save simulation metric",
                    request.getWarehouseId(),
                    e);
        }
    }

    public void trackDeviationAdjustment(final DeviationRequest deviationRequest) {

        final Tags tags = new Tags();

        tags.add("warehouse_id", deviationRequest.getWarehouseId());
        tags.add("duration", isTwentyFourHourRange(deviationRequest));

        tags.add("scope", getScope());

        try {
            datadog.incrementCounter("application.planning.model.deviation", tags);
        } catch (Exception e) {
            log.warn("[warehouse_id:{}][deviation:{}]"
                            + "Couldn't track deviation adjustment metric",
                    deviationRequest.getWarehouseId(),
                    deviationRequest.getValue(),
                    e);
        }
    }

    public void trackProjection(final String warehouseId,
                                final Workflow workflow,
                                final String projectionType) {

        final Tags tags = new Tags();

        tags.add("warehouse_id", warehouseId);
        tags.add("workflow", workflow.getName());
        tags.add("projection_type", projectionType);

        tags.add("scope", getScope());

        try {
            datadog.incrementCounter("application.planning.model.projection", tags);
        } catch (Exception e) {
            log.warn("[warehouse_id:{}][workflow:{}][projection_type:{}]"
                            + "Couldn't track projection metric",
                    warehouseId,
                    workflow.getName(),
                    projectionType,
                    e);
        }
    }

    public void trackForecastUpload(final String warehouseId) {
        final Tags tags = new Tags();
        tags.add("warehouse_id", warehouseId);
        tags.add("scope", getScope());

        try {
            datadog.incrementCounter("application.planning.model.forecast.upload", tags);
        } catch (Exception e) {
            log.warn("[warehouse_id:{}] Couldn't track forecast upload metric",
                    warehouseId, e);
        }
    }

    private Tags createSimulationTags(String warehouseId,
                                      List<SimulationRequest> simulationRequests) {
        final Tags tags = new Tags();

        tags.add("warehouse_id", warehouseId);
        simulationRequests.stream()
                .forEach(simulation -> tags.add(simulation.getProcessName(),
                        getLabelFromEntities(simulation.getEntities())));

        tags.add("scope", getScope());

        return tags;
    }

    private String getLabelFromEntities(final List<EntityRequest> entities) {
        return entities
                .stream()
                .map(entity -> entity.getType())
                .collect(Collectors.joining("-"));
    }

    private boolean isTwentyFourHourRange(final DeviationRequest request) {
        return Duration.between(request.getDateFrom(), request.getDateTo()).toHours() == 24;
    }
}
