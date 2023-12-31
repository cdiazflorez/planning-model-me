package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Productivity extends MagnitudePhoto {

    private int abilityLevel;

}
