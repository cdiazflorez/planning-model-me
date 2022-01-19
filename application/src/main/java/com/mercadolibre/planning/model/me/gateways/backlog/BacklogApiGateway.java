package com.mercadolibre.planning.model.me.gateways.backlog;

import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;

import java.util.List;

public interface BacklogApiGateway {
    List<Consolidation> getBacklog(BacklogRequest request);
}
