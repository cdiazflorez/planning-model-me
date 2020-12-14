package com.mercadolibre.planning.model.me.clients.groot;

import com.mercadolibre.kraken.client.clients.KrakenAttributesClient;
import com.mercadolibre.kraken.client.clients.KrakenUserClient;
import com.mercadolibre.planning.model.me.gateways.authorization.AuthorizationGateway;
import com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserAuthorization;
import com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KrakenClientTest {

    private KrakenAttributesClient krakenAttributesClient;

    private KrakenUserClient krakenUserClient;

    private AuthorizationGateway krakenClient;

    public KrakenClientTest() {
        krakenAttributesClient = Mockito.mock(KrakenAttributesClient.class);
        krakenUserClient = Mockito.mock(KrakenUserClient.class);
        krakenClient = new KrakenClient(krakenAttributesClient, krakenUserClient);
    }

    @Test
    public void testGetInactiveUser() {
        //GIVEN
        Long userId = 123L;
        Mockito.when(krakenUserClient.getUser(userId)).thenReturn(getUserAnswer(userId, false));

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
        Mockito.when(krakenUserClient.getUser(userId)).thenReturn(getUserAnswer(userId, true));
        Mockito.when(krakenUserClient.getAssignedPermissions(userId)).thenReturn(getPermissions());
        Mockito.when(krakenAttributesClient.getUserAttribute(userId, "warehouse"))
                .thenReturn(getAttribute());

        //WHEN
        UserAuthorization userAuthorization = krakenClient.get(userId);

        //THEN
        assertEquals(userId, userAuthorization.getUserId());
        assertEquals(userAuthorization.getPermissions().size(), 2);
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
