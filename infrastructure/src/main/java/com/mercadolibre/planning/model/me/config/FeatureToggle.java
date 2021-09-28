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

    public boolean hasBacklogMonitorFeatureEnabled(String warehouseId) {
        return backlogMonitorEnabledWarehouses.contains(warehouseId);
    }
}
