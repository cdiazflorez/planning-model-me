package com.mercadolibre.planning.model.me.clients.rest.planningmodel.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ProductivityResponse extends EntityResponse {

    private int abilityLevel;
}
