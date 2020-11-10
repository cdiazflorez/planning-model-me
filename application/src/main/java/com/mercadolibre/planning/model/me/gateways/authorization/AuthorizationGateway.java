package com.mercadolibre.planning.model.me.gateways.authorization;

import com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserAuthorization;

public interface AuthorizationGateway {

    UserAuthorization get(final Long userId);
}
