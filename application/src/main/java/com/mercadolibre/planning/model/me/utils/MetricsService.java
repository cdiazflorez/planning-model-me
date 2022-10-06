package com.mercadolibre.planning.model.me.utils;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;

/**
 * Send metrics to data dog.
 */
public interface MetricsService {

    void trackProjectionError(String warehouseId, Workflow workflow, String projectionType, String errorType);
}
