package com.mercadolibre.planning.model.me.entities.projection.complextable;

import com.mercadolibre.planning.model.me.entities.projection.Content;
import java.util.List;
import java.util.Map;
import lombok.Value;

@Value
public class Data {

    private String id;

    private String title;

    private boolean open;

    private List<Map<String, Content>> content;
}
