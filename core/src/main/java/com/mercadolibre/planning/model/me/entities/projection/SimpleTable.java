package com.mercadolibre.planning.model.me.entities.projection;

import java.util.List;
import java.util.Map;
import lombok.Value;

@Value
public class SimpleTable {

    String title;

    List<ColumnHeader> columns;

    List<Map<String,Object>> data;
}
