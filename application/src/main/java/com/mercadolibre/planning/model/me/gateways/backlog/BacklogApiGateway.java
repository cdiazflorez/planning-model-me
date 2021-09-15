package com.mercadolibre.planning.model.me.gateways.backlog;

import com.mercadolibre.planning.model.me.gateways.backlog.dto.Backlog;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogRequest;

import java.util.List;

public interface BacklogApiGateway {
    List<Backlog> getBacklog(BacklogRequest request);
}
