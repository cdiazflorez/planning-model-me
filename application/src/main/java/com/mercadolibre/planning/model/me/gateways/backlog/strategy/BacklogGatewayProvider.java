package com.mercadolibre.planning.model.me.gateways.backlog.strategy;

import com.mercadolibre.planning.model.me.gateways.backlog.BacklogGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.AllArgsConstructor;

import javax.inject.Named;

import java.util.Optional;
import java.util.Set;

@Named
@AllArgsConstructor
public class BacklogGatewayProvider {

    private final Set<BacklogGateway> backlogGateways;

    public Optional<BacklogGateway> getBy(final Workflow workflow) {
        return backlogGateways.stream()
                .filter(gateway -> gateway.supportsWorkflow(workflow))
                .findFirst();
    }
}
