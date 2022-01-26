package com.mercadolibre.planning.model.me.gateways.backlog;

import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;

import java.time.Instant;
import java.util.List;

public interface BacklogApiGateway {

    List<Consolidation> getBacklog(BacklogRequest request);

    List<Consolidation> getCurrentBacklog(final String logisticCenterId,
                                          final List<String> workflows,
                                          final List<String> steps,
                                          final Instant slaFrom,
                                          final Instant slaTo,
                                          final List<String> groupingFields);
}
