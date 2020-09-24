package com.mercadolibre.planning.model.me.clients.rest.authorization.response;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class AuthorizationResponse {

    private long userId;
    private List<String> warehouses;
    private List<String> permissions;
}
