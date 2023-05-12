package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;

@Value
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ProductivityRequest extends TrajectoriesRequest {

    private List<Integer> abilityLevel;

}
