package com.mercadolibre.planning.model.me.clients.rest.staffing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpClient;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpRequest;
import com.mercadolibre.fbm.wms.outbound.commons.rest.RequestBodyHandler;
import com.mercadolibre.json.type.TypeReference;
import com.mercadolibre.planning.model.me.clients.rest.staffing.request.StaffingRequest;
import com.mercadolibre.planning.model.me.gateways.staffing.StaffingGateway;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.request.GetStaffingRequest;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.StaffingResponse;
import com.mercadolibre.restclient.MeliRestClient;
import com.mercadolibre.restclient.exception.ParseException;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

import static com.mercadolibre.planning.model.me.clients.rest.config.RestPool.STAFFING;
import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Map.entry;
import static org.springframework.http.HttpStatus.OK;

@Component
public class StaffingApiClient extends HttpClient implements StaffingGateway {

    private static final String STAFFING_URL = "/logistic_centers/%s/metrics";
    private static final DateTimeFormatter DATE_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    private final ObjectMapper objectMapper;

    public StaffingApiClient(final MeliRestClient client, final ObjectMapper mapper) {
        super(client, STAFFING.name());
        this.objectMapper = mapper;
    }

    @Override
    public StaffingResponse getStaffing(GetStaffingRequest getStaffingRequest) {

        Map<String, String> filters = Map.ofEntries(
                entry("synchronization_date_from",
                        getStaffingRequest.getSynchronizationDateFrom().format(DATE_FORMATTER)),
                entry("synchronization_date_to",
                        getStaffingRequest.getSynchronizationDateTo().format(DATE_FORMATTER)),
                entry("logistic_center_id",getStaffingRequest.getLogisticCenterId()));

        final HttpRequest request = HttpRequest.builder()
                .url(format(STAFFING_URL, getStaffingRequest.getLogisticCenterId()))
                .POST(requestSupplier(
                        new StaffingRequest(filters, getStaffingRequest.getAggregations())))
                .acceptedHttpStatuses(Set.of(OK))
                .build();

        final StaffingResponse apiResponse = send(request, response ->
                response.getData(new TypeReference<>() {}));

        return apiResponse;
    }

    private <T> RequestBodyHandler requestSupplier(final T requestBody) {
        return () -> {
            try {
                return objectMapper.writeValueAsBytes(requestBody);
            } catch (JsonProcessingException e) {
                throw new ParseException(e);
            }
        };
    }
}
