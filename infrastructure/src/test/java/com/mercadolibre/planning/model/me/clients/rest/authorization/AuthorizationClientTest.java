package com.mercadolibre.planning.model.me.clients.rest.authorization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.planning.model.me.clients.rest.BaseClientTest;
import com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserAuthorization;
import com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission;
import com.mercadolibre.restclient.MockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission.OUTBOUND_PROJECTION;
import static com.mercadolibre.restclient.http.ContentType.APPLICATION_JSON;
import static com.mercadolibre.restclient.http.ContentType.HEADER_NAME;
import static com.mercadolibre.restclient.http.HttpMethod.GET;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuthorizationClientTest extends BaseClientTest {

    private AuthorizationClient authorizationClient;

    @BeforeEach
    public void setUp() throws IOException {
        authorizationClient = new AuthorizationClient(getRestTestClient());
    }

    @Test
    @DisplayName("Get user roles for a given user returns them")
    public void testGetUserPermissions() throws JsonProcessingException {
        //GIVEN
        final Long userId = 1L;
        final List<UserPermission> expectedUserPermissions = List.of(OUTBOUND_PROJECTION);

        userPermissionsSuccessful(userId, expectedUserPermissions);

        //WHEN
        final UserAuthorization userRoles = authorizationClient.get(userId);

        //THEN
        assertEquals(expectedUserPermissions, userRoles.getPermissions());
    }

    public static void userPermissionsSuccessful(
            final Long userId,
            final List<UserPermission> userPermissions) throws JsonProcessingException {

        final Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("user_id", userId);
        responseBody.put("status", "active");
        responseBody.put("warehouses", singletonList("ARTW01"));
        responseBody.put("permissions", userPermissions);

        MockResponse.builder()
                .withMethod(GET)
                .withURL(format(BASE_URL + "/wms/users/%s", userId))
                .withStatusCode(HttpStatus.OK.value())
                .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
                .withResponseBody(new ObjectMapper().writeValueAsBytes(responseBody))
                .build();
    }
}
