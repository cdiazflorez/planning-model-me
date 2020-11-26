package com.mercadolibre.planning.model.me.usecases.authorization;

import com.mercadolibre.planning.model.me.gateways.authorization.AuthorizationGateway;
import com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserAuthorization;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.authorization.dtos.AuthorizeUserDto;
import com.mercadolibre.planning.model.me.usecases.authorization.exceptions.UserNotAuthorizedException;
import lombok.AllArgsConstructor;

import javax.inject.Named;

import static java.lang.String.format;

@Named
@AllArgsConstructor
public class AuthorizeUser implements UseCase<AuthorizeUserDto, Void> {

    private AuthorizationGateway authorizationGateway;

    @Override
    public Void execute(AuthorizeUserDto input) {
        final Long userId = input.getUserId();
        final UserAuthorization userAuthorization = authorizationGateway.get(userId);

        if (!userAuthorization.getPermissions().containsAll(input.getRequiredPermissions())) {
            throw new UserNotAuthorizedException(format("User %s has no permissions.", userId));
        }

        return null;
    }
}
