package com.mercadolibre.planning.model.me.clients.groot;

import com.mercadolibre.kraken.client.clients.KrakenAttributesClient;
import com.mercadolibre.kraken.client.clients.KrakenUserClient;
import com.mercadolibre.planning.model.me.gateways.authorization.AuthorizationGateway;
import com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserAuthorization;
import com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission;
import com.mercadolibre.restclient.util.MeliContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KrakenClientTest {

    private KrakenAttributesClient krakenAttributesClient;

    private KrakenUserClient krakenUserClient;

    private AuthorizationGateway krakenClient;

    public KrakenClientTest() {
        krakenAttributesClient = mock(KrakenAttributesClient.class);
        krakenUserClient = mock(KrakenUserClient.class);
        krakenClient = new KrakenClient(krakenAttributesClient, krakenUserClient);
    }

    @Test
    public void testGetInactiveUser() {
        //GIVEN
        Long userId = 123L;
        when(krakenUserClient.getUser(eq(userId), any(MeliContext.class)))
                .thenReturn(getUserAnswer(userId, false));

        //WHEN
        UserAuthorization userAuthorization = krakenClient.get(userId);

        //THEN
        assertEquals(userId, userAuthorization.getUserId());
        org.junit.jupiter.api.Assertions.assertTrue(userAuthorization.getPermissions().isEmpty());
    }

    @Test
    public void testGetUser() {
        //GIVEN
        Long userId = 123L;
        when(krakenUserClient.getUser(eq(userId), any(MeliContext.class)))
                .thenReturn(getUserAnswer(userId, true));

        when(krakenUserClient.getAssignedPermissions(eq(userId), any(MeliContext.class)))
                .thenReturn(getPermissions());

        when(krakenAttributesClient.getUserAttribute(
                eq(userId), eq("warehouse"), any(MeliContext.class)))
                .thenReturn(getAttribute());

        //WHEN
        UserAuthorization userAuthorization = krakenClient.get(userId);

        //THEN
        assertEquals(userId, userAuthorization.getUserId());
        assertEquals( 2, userAuthorization.getPermissions().size());
        assertTrue(userAuthorization.getPermissions().contains(UserPermission.UNKNOWN));
        assertTrue(userAuthorization.getPermissions().contains(UserPermission.OUTBOUND_SIMULATION));

    }

    private Map getUserAnswer(Long userId, Boolean active) {
        Map<String, Object> map = Map.of("active", active, "id", userId);
        return map;
    }

    private List<Map> getPermissions() {
        return List.of(Map.of("key", "cubing_write"),
                Map.of("key", "checkin_write"),
                Map.of("key", "outbound_simulation"));
    }

    private Map getAttribute() {
        return Map.of("key", "warehouse", "value", List.of("MXCD01", "ARBA01"));
    }
}
