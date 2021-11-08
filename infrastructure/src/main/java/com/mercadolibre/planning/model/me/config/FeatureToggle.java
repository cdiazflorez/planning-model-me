package com.mercadolibre.planning.model.me.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.Set;

@RefreshScope
@Component
public class FeatureToggle {

    @Value("${backlog.monitor.enabled-warehouses}")
    private Set<String> backlogMonitorEnabledWarehouses;

    @Value("${newcap5.logistic-center}")
    private Set<String> newCap5LogisticCenters;

    public boolean hasBacklogMonitorFeatureEnabled(String warehouseId) {
        return backlogMonitorEnabledWarehouses.contains(warehouseId);
    }

    public boolean hasNewCap5Logic(String logisticCenterId) {
        return newCap5LogisticCenters.contains(logisticCenterId);
    }
}
