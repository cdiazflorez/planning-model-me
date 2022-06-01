package com.mercadolibre.planning.model.me.entities.projection.simulationmode;

import java.util.List;
import lombok.Value;

@Value
public class MagnitudeValidate {

    String type;

    List<DateValidate> values;
}
