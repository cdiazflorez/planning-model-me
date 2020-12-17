package com.mercadolibre.planning.model.me.gateways.analytics;

import com.mercadolibre.planning.model.me.entities.projection.AnalyticsQueryEvent;
import com.mercadolibre.planning.model.me.entities.projection.UnitsResume;

import java.util.List;

public interface AnalyticsGateway {

    List<UnitsResume> getUnitsInInterval(final String warehouseId, 
            final int hoursOffset, final List<AnalyticsQueryEvent> eventType);
}
