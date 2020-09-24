package com.mercadolibre.planning.model.me.usecases;

import com.mercadolibre.planning.model.me.gateways.authorization.AuthorizationGateway;
import com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserAuthorization;
import com.mercadolibre.planning.model.me.usecases.authorization.AuthorizeUser;
import com.mercadolibre.planning.model.me.usecases.authorization.dtos.AuthorizeUserDto;
import com.mercadolibre.planning.model.me.usecases.authorization.exceptions.UserNotAuthorizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission.WAVE_READ;
import static com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission.WAVE_WRITE;
import static com.mercadolibre.planning.model.me.utils.TestUtils.USER_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthorizeUserTest {

    @InjectMocks
    private AuthorizeUser authorizeUser;

    @Mock
    private AuthorizationGateway authorizationGateway;

    @Test
    public void testExecuteOk() {
        // GIVEN
        final AuthorizeUserDto authorizeUserDto = AuthorizeUserDto.builder()
                .userId(USER_ID)
                .requiredPermissions(List.of(WAVE_WRITE))
                .build();

        when(authorizationGateway.get(USER_ID))
                .thenReturn(new UserAuthorization(
                        USER_ID,
                        List.of(WAREHOUSE_ID),
                        List.of(WAVE_WRITE, WAVE_READ)
                ));

        // WHEN - THEN
        assertDoesNotThrow(() -> authorizeUser.execute(authorizeUserDto));
    }

    @Test
    public void testExecuteNotAuthorized() {
        // GIVEN
        final AuthorizeUserDto authorizeUserDto = AuthorizeUserDto.builder()
                .userId(USER_ID)
                .requiredPermissions(List.of(WAVE_WRITE))
                .build();

        when(authorizationGateway.get(USER_ID))
                .thenReturn(new UserAuthorization(
                        USER_ID,
                        List.of(WAREHOUSE_ID),
                        List.of(WAVE_READ)
                ));

        // WHEN - THEN
        assertThrows(UserNotAuthorizedException.class,
                () -> authorizeUser.execute(authorizeUserDto));
    }
}
