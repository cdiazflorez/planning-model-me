package com.mercadolibre.planning.model.me.clients.rest.authorization;

import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpClient;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpRequest;
import com.mercadolibre.json.type.TypeReference;
import com.mercadolibre.planning.model.me.clients.rest.authorization.response.AuthorizationResponse;
import com.mercadolibre.planning.model.me.clients.rest.config.RestPool;
import com.mercadolibre.planning.model.me.gateways.authorization.AuthorizationGateway;
import com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserAuthorization;
import com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission;
import com.mercadolibre.restclient.RestClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Set;

import static java.util.stream.Collectors.toList;


//@Component
public class AuthorizationClient extends HttpClient implements AuthorizationGateway {

    private static final String URL = "/wms/users/%s";

    public AuthorizationClient(final RestClient restClient) {
        super(restClient, RestPool.AUTHORIZATION.name());
    }

    public UserAuthorization get(final Long userId) {
        final HttpRequest request = HttpRequest.builder()
                .url(String.format(URL, userId))
                .GET()
                .acceptedHttpStatuses(Set.of(HttpStatus.OK))
                .build();

        return toUserAuthorization(send(request, response ->
                response.getData(new TypeReference<>() {})));
    }

    private UserAuthorization toUserAuthorization(final AuthorizationResponse response) {
        return new UserAuthorization(
                response.getUserId(),
                response.getWarehouses(),
                response.getPermissions().stream().map(UserPermission::from).collect(toList())
        );
    }
}
