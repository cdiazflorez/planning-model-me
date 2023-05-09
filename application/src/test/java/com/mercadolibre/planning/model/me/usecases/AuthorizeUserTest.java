package com.mercadolibre.planning.model.me.usecases;

import static com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission.OUTBOUND_PROJECTION;
import static com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission.OUTBOUND_SIMULATION;
import static com.mercadolibre.planning.model.me.utils.TestUtils.USER_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.gateways.authorization.AuthorizationGateway;
import com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserAuthorization;
import com.mercadolibre.planning.model.me.usecases.authorization.AuthorizeUser;
import com.mercadolibre.planning.model.me.usecases.authorization.dtos.AuthorizeUserDto;
import com.mercadolibre.planning.model.me.usecases.authorization.exceptions.UserNotAuthorizedException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AuthorizeUserTest {

    @InjectMocks
    private AuthorizeUser authorizeUser;

    @Mock
    private AuthorizationGateway authorizationGateway;

    @Test
    public void testExecuteOk() {
        // GIVEN
        final AuthorizeUserDto authorizeUserDto = new AuthorizeUserDto(USER_ID,
                List.of(OUTBOUND_SIMULATION));

        when(authorizationGateway.get(USER_ID))
                .thenReturn(new UserAuthorization(
                        USER_ID,
                        List.of(WAREHOUSE_ID),
                        List.of(OUTBOUND_SIMULATION, OUTBOUND_PROJECTION)
                ));

        // WHEN - THEN
        assertDoesNotThrow(() -> authorizeUser.execute(authorizeUserDto));
    }

    @Test
    public void testExecuteNotAuthorized() {
        // GIVEN
        final AuthorizeUserDto authorizeUserDto = new AuthorizeUserDto(USER_ID,
                List.of(OUTBOUND_SIMULATION));

        when(authorizationGateway.get(USER_ID))
                .thenReturn(new UserAuthorization(
                        USER_ID,
                        List.of(WAREHOUSE_ID),
                        List.of(OUTBOUND_PROJECTION)
                ));

        // WHEN - THEN
        assertThrows(UserNotAuthorizedException.class,
                () -> authorizeUser.execute(authorizeUserDto));
    }
}
