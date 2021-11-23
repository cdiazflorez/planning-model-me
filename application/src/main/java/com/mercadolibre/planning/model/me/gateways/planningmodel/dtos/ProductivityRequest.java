package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Value
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ProductivityRequest extends TrajectoriesRequest {

    private List<Integer> abilityLevel;

}
