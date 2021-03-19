package com.mercadolibre.planning.model.me.controller.simulation;

import com.mercadolibre.planning.model.me.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.me.controller.simulation.request.RunSimulationRequest;
import com.mercadolibre.planning.model.me.controller.simulation.request.SaveSimulationRequest;
import com.mercadolibre.planning.model.me.controller.simulation.request.SimulationRequest;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Simulation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.metric.DatadogMetricService;
import com.mercadolibre.planning.model.me.usecases.authorization.AuthorizeUser;
import com.mercadolibre.planning.model.me.usecases.authorization.dtos.AuthorizeUserDto;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.projection.simulation.RunSimulation;
import com.mercadolibre.planning.model.me.usecases.projection.simulation.SaveSimulation;
import com.newrelic.api.agent.Trace;
import lombok.AllArgsConstructor;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import java.util.List;
import java.util.Optional;

import static com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission.OUTBOUND_SIMULATION;
import static java.util.stream.Collectors.toList;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/middleend/workflows/{workflow}/simulations")
public class SimulationController {

    private final RunSimulation runSimulation;
    private final SaveSimulation saveSimulation;
    private final AuthorizeUser authorizeUser;
    private final DatadogMetricService datadogMetricService;

    @Trace
    @PostMapping("/run")
    public ResponseEntity<Projection> run(
            @PathVariable final Workflow workflow,
            @RequestParam("caller.id") final @NotNull Long callerId,
            @RequestBody @Valid final RunSimulationRequest request) {

        authorizeUser.execute(new AuthorizeUserDto(callerId, List.of(OUTBOUND_SIMULATION)));

        datadogMetricService.trackRunSimulation(request);

        return ResponseEntity.of(Optional.of(runSimulation.execute(GetProjectionInputDto.builder()
                .workflow(workflow)
                .userId(callerId)
                .warehouseId(request.getWarehouseId())
                .simulations(fromRequest(request.getSimulations()))
                .build()))
        );
    }

    @Trace
    @PostMapping("/save")
    public ResponseEntity<Projection> save(
            @PathVariable final Workflow workflow,
            @RequestParam("caller.id") final @NotNull Long callerId,
            @RequestBody final SaveSimulationRequest request) {

        authorizeUser.execute(new AuthorizeUserDto(callerId, List.of(OUTBOUND_SIMULATION)));

        datadogMetricService.trackSaveSimulation(request);

        return ResponseEntity.of(Optional.of(saveSimulation.execute(GetProjectionInputDto.builder()
                .workflow(workflow)
                .warehouseId(request.getWarehouseId())
                .userId(callerId)
                .simulations(fromRequest(request.getSimulations()))
                .build())));
    }

    private List<Simulation> fromRequest(final List<SimulationRequest> simulationRequests) {
        return simulationRequests.stream()
                .map(SimulationRequest::toSimulation)
                .collect(toList());
    }

    @InitBinder
    public void initBinder(final PropertyEditorRegistry dataBinder) {
        dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
    }
}
