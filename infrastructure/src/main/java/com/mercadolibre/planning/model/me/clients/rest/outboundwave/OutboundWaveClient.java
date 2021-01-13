package com.mercadolibre.planning.model.me.clients.rest.outboundwave;

import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpClient;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpRequest;
import com.mercadolibre.json.type.TypeReference;
import com.mercadolibre.planning.model.me.clients.rest.config.RestPool;
import com.mercadolibre.planning.model.me.entities.projection.UnitsResume;
import com.mercadolibre.planning.model.me.gateways.outboundwave.OutboundWaveGateway;
import com.mercadolibre.restclient.RestClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

@Slf4j
@Component
public class OutboundWaveClient extends HttpClient implements OutboundWaveGateway {

    private static final String WAVE_CONSIDERED_UNITS_COUNT_URL =
            "/wms/outbound/wave_considered_units/count";

    protected OutboundWaveClient(RestClient client) {
        super(client, RestPool.OUTBOUND_WAVE.name());
    }

    @Override
    public UnitsResume getUnitsCount(final String warehouseId,
                                     final ZonedDateTime dateFrom,
                                     final ZonedDateTime dateTo,
                                     final String unitGroupType) {
        final Map<String, String> params = new HashMap<>();
        params.put("warehouse_id", warehouseId);
        params.put("date_from", dateFrom.format(ISO_OFFSET_DATE_TIME));
        params.put("date_to", dateTo.format(ISO_OFFSET_DATE_TIME));
        params.put("unit_group_type", unitGroupType);

        final HttpRequest request = HttpRequest.builder()
                .url(WAVE_CONSIDERED_UNITS_COUNT_URL)
                .GET()
                .queryParams(params)
                .acceptedHttpStatuses(Set.of(HttpStatus.OK))
                .build();
        return UnitsResume.builder()
                .unitCount(send(request, response ->
                response.getData(new TypeReference<>() {})))
                .build();
    }
}
