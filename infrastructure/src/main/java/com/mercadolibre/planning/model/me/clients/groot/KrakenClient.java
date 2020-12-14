package com.mercadolibre.planning.model.me.clients.groot;

import com.mercadolibre.kraken.client.clients.KrakenAttributesClient;
import com.mercadolibre.kraken.client.clients.KrakenUserClient;
import com.mercadolibre.planning.model.me.gateways.authorization.AuthorizationGateway;
import com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserAuthorization;
import com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KrakenClient implements AuthorizationGateway {

    private static final String WAREHOUSE_ATTRIBUTE = "warehouse";

    private KrakenUserClient krakenUserClient;
    private KrakenAttributesClient krakenAttributesClient;

    public KrakenClient(KrakenAttributesClient krakenAttributesClient,
                        KrakenUserClient krakenUserClient) {
        super();
        this.krakenAttributesClient = krakenAttributesClient;
        this.krakenUserClient = krakenUserClient;
    }

    @Override
    public UserAuthorization get(Long userId) {
        Map user = krakenUserClient.getUser(userId);
        if (isUserActive(user)) {
            Map warehouseAttribute
                    = krakenAttributesClient.getUserAttribute(userId, WAREHOUSE_ATTRIBUTE);
            List<Map> permissions = krakenUserClient.getAssignedPermissions(userId);
            return new UserAuthorization(userId,
                    getWarehouses(warehouseAttribute),
                    getPermissions(permissions));
        } else {
            return new UserAuthorization(userId, Collections.emptyList(), Collections.emptyList());
        }
    }

    private boolean isUserActive(Map user) {
        return (Boolean) user.getOrDefault("active", true);
    }

    private List<String> getWarehouses(Map warehouseAttribute) {
        return (List<String>) warehouseAttribute.getOrDefault("value", Collections.emptyList());
    }

    private List<UserPermission> getPermissions(List<Map> permissions) {
        return permissions.stream()
                .map((it) -> UserPermission.from((String) it.get("key")))
                .distinct()
                .collect(Collectors.toList());
    }
}
