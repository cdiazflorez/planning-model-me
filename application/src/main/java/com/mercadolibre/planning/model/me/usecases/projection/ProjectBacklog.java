package com.mercadolibre.planning.model.me.usecases.projection;

import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.CurrentBacklog;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.BacklogProjectionInput;
import com.mercadolibre.planning.model.me.usecases.projection.entities.ProjectedBacklog;
import lombok.AllArgsConstructor;

import javax.inject.Named;

import java.util.List;
import java.util.stream.Collectors;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.BacklogProjectionRequest.fromInput;

@Named
@AllArgsConstructor
public class ProjectBacklog implements UseCase<BacklogProjectionInput, ProjectedBacklog> {

    private final PlanningModelGateway planningModel;

    @Override
    public ProjectedBacklog execute(final BacklogProjectionInput input) {

        final List<CurrentBacklog> backlogs = mapBacklogByProcesses(input.getCurrentBacklog(), input.getProcessName());
        return new ProjectedBacklog(planningModel.getBacklogProjection(fromInput(input, backlogs)));
    }

    private List<CurrentBacklog> mapBacklogByProcesses(final List<Consolidation> currentBacklog,
                                                       final List<ProcessName> processes) {

        return processes.stream()
                .map(process -> new CurrentBacklog(process, currentBacklog.stream()
                .filter(item -> process.getName().equals(item.getKeys().get("process")))
                .findFirst()
                .map(Consolidation::getTotal)
                .orElse(0)))
                .collect(Collectors.toList());
    }
}

