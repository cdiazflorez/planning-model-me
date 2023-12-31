package com.mercadolibre.planning.model.me.controller.simulation;

import static com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission.OUTBOUND_SIMULATION;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

import com.mercadolibre.planning.model.me.controller.RequestClock;
import com.mercadolibre.planning.model.me.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.me.controller.simulation.request.RunSimulationRequest;
import com.mercadolibre.planning.model.me.controller.simulation.request.SaveSimulationRequest;
import com.mercadolibre.planning.model.me.controller.simulation.request.SimulationRequest;
import com.mercadolibre.planning.model.me.entities.projection.PlanningView;
import com.mercadolibre.planning.model.me.entities.projection.simulationmode.ValidatedMagnitude;
import com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Simulation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.metric.DatadogMetricService;
import com.mercadolibre.planning.model.me.usecases.authorization.AuthorizeUser;
import com.mercadolibre.planning.model.me.usecases.authorization.dtos.AuthorizeUserDto;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetDeferralProjection;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetProjectionInput;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.projection.simulation.RunSimulation;
import com.mercadolibre.planning.model.me.usecases.projection.simulation.SaveSimulation;
import com.mercadolibre.planning.model.me.usecases.projection.simulation.ValidateSimulationService;
import com.mercadolibre.planning.model.me.usecases.projection.simulation.WriteSimulation;
import com.newrelic.api.agent.Trace;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/middleend/workflows/{workflow}/simulations")
public class SimulationController {
  private static final Map<Workflow, List<UserPermission>> USER_PERMISSION = Map.of(
      Workflow.FBM_WMS_INBOUND, List.of(OUTBOUND_SIMULATION),
      Workflow.FBM_WMS_OUTBOUND, List.of(OUTBOUND_SIMULATION));
  private static final String CALLER_ID = "caller.id";
  private final RunSimulation runSimulation;
  private final SaveSimulation saveSimulation;
  private final AuthorizeUser authorizeUser;
  private final DatadogMetricService datadogMetricService;
  private final RequestClock requestClock;
  private final ValidateSimulationService validateSimulationService;
  private final GetDeferralProjection getDeferralProjection;
  private final WriteSimulation writeSimulation;

  @Trace
  @PostMapping("/run")
  public ResponseEntity<PlanningView> run(
      @PathVariable final Workflow workflow,
      @RequestParam(CALLER_ID) final @NotNull Long callerId,
      @RequestBody @Valid final RunSimulationRequest request) {

    authorizeUser.execute(new AuthorizeUserDto(callerId, USER_PERMISSION.get(workflow)));

    datadogMetricService.trackRunSimulation(request);

    return ResponseEntity.of(of(runSimulation.execute(GetProjectionInputDto.builder()
        .workflow(workflow)
        .userId(callerId)
        .warehouseId(request.getWarehouseId())
        .simulations(fromRequest(request.getSimulations()))
        .date(request.getDate())
        .requestDate(requestClock.now())
        .build()))
    );
  }

  @Trace
  @PostMapping("/save")
  public ResponseEntity<PlanningView> save(
      @PathVariable final Workflow workflow,
      @RequestParam(CALLER_ID) final @NotNull Long callerId,
      @RequestBody final SaveSimulationRequest request) {

    authorizeUser.execute(new AuthorizeUserDto(callerId, USER_PERMISSION.get(workflow)));

    datadogMetricService.trackSaveSimulation(request);

    return ResponseEntity.of(of(saveSimulation.execute(GetProjectionInputDto.builder()
        .workflow(workflow)
        .warehouseId(request.getWarehouseId())
        .userId(callerId)
        .simulations(fromRequest(request.getSimulations()))
        .date(request.getDate())
        .requestDate(requestClock.now())
        .build())));
  }

  @Trace
  @PostMapping("/deferral/validate")
  public ResponseEntity<List<ValidatedMagnitude>> validate(@PathVariable final Workflow workflow,
                                                           @RequestParam(CALLER_ID) final @NotNull Long callerId,
                                                           @RequestBody final RunSimulationRequest request) {

    authorizeUser.execute(new AuthorizeUserDto(callerId, USER_PERMISSION.get(workflow)));

    return ResponseEntity.of(of(validateSimulationService.execute(GetProjectionInputDto.builder()
        .workflow(workflow)
        .warehouseId(request.getWarehouseId())
        .simulations(fromRequest(request.getSimulations()))
        .build())));
  }

  @Trace
  @PostMapping("/deferral/run")
  public ResponseEntity<PlanningView> runDeferralProjection(
      @PathVariable final Workflow workflow,
      @RequestParam(CALLER_ID) @NotNull final Long callerId,
      @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME) final ZonedDateTime date,
      @RequestParam(required = false, defaultValue = "false") boolean cap5ToPack,
      @RequestBody final RunSimulationRequest request) {

    authorizeUser.execute(new AuthorizeUserDto(callerId, USER_PERMISSION.get(workflow)));

    datadogMetricService.trackProjectionRequest(request.getWarehouseId(), workflow, "trackRunSimulation");

    return ResponseEntity.of(of(getDeferralProjection.execute(new GetProjectionInput(
        request.getWarehouseId(),
        workflow,
        date,
        null,
        cap5ToPack,
        fromRequest(request.getSimulations())
    ))));
  }

  @Trace
  @PostMapping("/deferral/save")
  public ResponseEntity<PlanningView> saveDeferralProjection(
      @PathVariable final Workflow workflow,
      @RequestParam(CALLER_ID) @NotNull final Long callerId,
      @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME) final ZonedDateTime date,
      @RequestParam(required = false, defaultValue = "false") boolean cap5ToPack,
      @RequestBody final RunSimulationRequest request) {

    authorizeUser.execute(new AuthorizeUserDto(callerId, USER_PERMISSION.get(workflow)));

    datadogMetricService.trackProjectionRequest(request.getWarehouseId(), workflow, "trackSaveSimulation");

    writeSimulation.saveSimulations(workflow, request.getWarehouseId(), fromRequest(request.getSimulations()), callerId);

    return ResponseEntity.of(of(getDeferralProjection.execute(new GetProjectionInput(
        request.getWarehouseId(),
        workflow,
        date,
        null,
        cap5ToPack,
        fromRequest(request.getSimulations())
    ))));
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
