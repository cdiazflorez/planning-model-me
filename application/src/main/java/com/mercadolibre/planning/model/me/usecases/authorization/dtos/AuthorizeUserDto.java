package com.mercadolibre.planning.model.me.usecases.authorization.dtos;

import com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AuthorizeUserDto {
    private Long userId;
    private List<UserPermission> requiredPermissions;
}
