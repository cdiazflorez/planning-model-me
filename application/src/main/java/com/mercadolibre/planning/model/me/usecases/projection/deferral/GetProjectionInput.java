package com.mercadolibre.planning.model.me.usecases.projection.deferral;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Simulation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.Value;

@Value
public class GetProjectionInput {

    private String logisticCenterId;

    private Workflow workflow;

    private ZonedDateTime date;

    private List<Backlog> backlogToProject;

    private boolean wantToSimulate21;

    private List<Simulation> simulations;
}
