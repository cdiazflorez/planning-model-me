package com.mercadolibre.planning.model.me.entities.projection.simulationmode;

import java.util.List;
import lombok.Value;

@Value
public class ValidatedMagnitude {

    String type;

    List<DateValidate> values;
}
