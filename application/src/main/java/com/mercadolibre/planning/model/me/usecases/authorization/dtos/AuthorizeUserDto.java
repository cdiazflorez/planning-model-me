package com.mercadolibre.planning.model.me.usecases.authorization.dtos;

import com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthorizeUserDto {

    private Long userId;
    private List<UserPermission> requiredPermissions;
}
