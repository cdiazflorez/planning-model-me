package com.mercadolibre.planning.model.me.usecases.projection.deferral;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

@Value
public class GetProjectionInput {

    private String logisticCenterId;

    private Workflow workflow;

    private ZonedDateTime date;

    private List<Backlog> backlogToProject;

    private boolean wantToSimulate21;
}
