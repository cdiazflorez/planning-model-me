package com.mercadolibre.planning.model.me.gateways.authorization.dtos;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Value;

@Value
public class UserAuthorization {
    private long userId;
    private List<String> warehouses;
    private List<UserPermission> permissions;

    public List<UserPermission> getPermissions() {
        return permissions
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
