package com.mercadolibre.planning.model.me.entities.projection;

import lombok.Value;

@Value
public class SelectionValue {

    String id;

    String title;

    boolean selected;
}
