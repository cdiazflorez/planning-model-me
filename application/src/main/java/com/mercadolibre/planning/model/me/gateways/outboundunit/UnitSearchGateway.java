package com.mercadolibre.planning.model.me.gateways.outboundunit;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogFilters;

import java.util.List;

public interface UnitSearchGateway {

    List<Backlog> getSalesByCpt(BacklogFilters filters);
}
