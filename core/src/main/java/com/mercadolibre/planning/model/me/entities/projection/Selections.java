package com.mercadolibre.planning.model.me.entities.projection;

import java.util.List;
import lombok.Value;

@Value
public class Selections {

    String title;

    List<SelectionValue> values;
}
