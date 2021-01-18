package com.mercadolibre.planning.model.me.clients.rest.analytics;

import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpClient;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpRequest;
import com.mercadolibre.json.type.TypeReference;
import com.mercadolibre.planning.model.me.clients.rest.analytics.response.UnitsResumeResponse;
import com.mercadolibre.planning.model.me.clients.rest.config.RestPool;
import com.mercadolibre.planning.model.me.entities.projection.AnalyticsQueryEvent;
import com.mercadolibre.planning.model.me.entities.projection.UnitsResume;
import com.mercadolibre.planning.model.me.gateways.analytics.AnalyticsGateway;
import com.mercadolibre.restclient.RestClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Component
public class AnalyticsClient extends HttpClient implements AnalyticsGateway {

    private static final String ANALYTICS_URL = "/wms/warehouses/%s/metric/count";

    protected AnalyticsClient(RestClient client) {
        super(client, RestPool.ANALYTICS.name());
    }

    @Override
    public List<UnitsResume> getUnitsInInterval(String warehouseId, int hoursOffset,
            List<AnalyticsQueryEvent> eventType) {
        
        final Map<String, String> defaultParams = createUnitQueryParameters(hoursOffset, eventType);
        final HttpRequest request = HttpRequest.builder()
                .url(format(ANALYTICS_URL, warehouseId))
                .GET()
                .queryParams(defaultParams)
                .acceptedHttpStatuses(Set.of(HttpStatus.OK))
                .build();
        final List<UnitsResumeResponse> unitsResponse = send(request, 
                response -> response.getData(new TypeReference<>() {}));
                    
        return unitsResponse.stream().map(this::toUnitsResume).collect(Collectors.toList());
    }
    
    private UnitsResume toUnitsResume(UnitsResumeResponse unitResponse) {
        return UnitsResume.builder()
                .eventCount(unitResponse.getEventCount())
                .unitCount(unitResponse.getUnitCount())
                .process(AnalyticsQueryEvent.fromString(unitResponse.getProcess()))
                .build();
    }

    private Map<String, String> createUnitQueryParameters(int hoursOffset,
            List<AnalyticsQueryEvent> eventType) {
        final Map<String, String> defaultParams = new HashMap<>();
        defaultParams.put("hoursOffset", String.valueOf(hoursOffset));
        defaultParams.put("processNames", eventType.stream().map(AnalyticsQueryEvent::getName)
                .collect(Collectors.joining(",")));
        return defaultParams;
    }
    
}
