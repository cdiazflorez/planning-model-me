package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
public class Forecast {
    private List<Metadata> metadata;
    private long userID;
}
