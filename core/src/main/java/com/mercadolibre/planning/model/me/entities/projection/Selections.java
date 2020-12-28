package com.mercadolibre.planning.model.me.entities.projection;

import lombok.Value;

import java.util.List;

@Value
public class Selections {

    String title;

    List<SelectionValue> values;
}
