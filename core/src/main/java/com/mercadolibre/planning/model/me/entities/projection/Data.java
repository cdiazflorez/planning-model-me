package com.mercadolibre.planning.model.me.entities.projection;

import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
public class Data {

    private String id;

    private String title;

    private boolean open;

    private List<Map<String, Content>> content;
}
