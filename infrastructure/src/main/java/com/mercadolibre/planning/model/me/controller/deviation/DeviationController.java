package com.mercadolibre.planning.model.me.controller.deviation;

import static com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission.OUTBOUND_SIMULATION;
import static java.util.stream.Collectors.toList;

import com.mercadolibre.planning.model.me.controller.deviation.request.DisableDeviationRequest;
import com.mercadolibre.planning.model.me.controller.deviation.request.SaveDeviationRequest;
import com.mercadolibre.planning.model.me.controller.editor.DeviationTypeEditor;
import com.mercadolibre.planning.model.me.controller.editor.ShipmentTypeEditor;
import com.mercadolibre.planning.model.me.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.me.enums.DeviationType;
import com.mercadolibre.planning.model.me.enums.ShipmentType;
import com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.metric.DatadogMetricService;
import com.mercadolibre.planning.model.me.usecases.authorization.AuthorizeUser;
import com.mercadolibre.planning.model.me.usecases.authorization.dtos.AuthorizeUserDto;
import com.mercadolibre.planning.model.me.usecases.deviation.DisableDeviation;
import com.mercadolibre.planning.model.me.usecases.deviation.SaveDeviation;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.DisableDeviationInput;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.SaveDeviationInput;
import com.newrelic.api.agent.Trace;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@AllArgsConstructor
@Validated
@RestController
// TODO: Use traffic routes and remove "workflow" from path
@RequestMapping("/planning/model/middleend/workflows/{workflow}/deviations")
public class DeviationController {

    // TODO: Update Permissions when inbound-edition adoption improves
    private static final Map<Workflow, UserPermission> EDIT_PERMISSION_BY_WORKFLOW = Map.of(
            Workflow.FBM_WMS_OUTBOUND, OUTBOUND_SIMULATION,
            Workflow.FBM_WMS_INBOUND, OUTBOUND_SIMULATION,
            Workflow.INBOUND, OUTBOUND_SIMULATION,
            Workflow.INBOUND_TRANSFER, OUTBOUND_SIMULATION
    );

    private final SaveDeviation saveDeviation;

    private final DisableDeviation disableDeviation;

    private final AuthorizeUser authorizeUser;

    private final DatadogMetricService datadogMetricService;

    @Trace
    @PostMapping("/save/{type}/all")
    public ResponseEntity<Object> save(
            @PathVariable final DeviationType type,
            @RequestParam final String logisticCenterId,
            @RequestParam("caller.id") final long callerId,
            @RequestBody @Valid @NotEmpty final List<SaveDeviationRequest> deviations
    ) {
        final List<Workflow> workflows = deviations.stream()
                .map(SaveDeviationRequest::getWorkflow)
                .collect(toList());

        checkUserPermissions(callerId, workflows);

        final List<SaveDeviationInput> deviationInputs = deviations.stream()
                .map(deviation -> deviation.toDeviationInput(logisticCenterId, callerId, type))
                .collect(toList());

        saveDeviation.execute(deviationInputs);

        return ResponseEntity.noContent().build();
    }

  @Trace
  @PostMapping("/disable/{type}/all")
  public ResponseEntity<Object> disable(
      @RequestParam final String logisticCenterId,
      @RequestParam("caller.id") @NotNull final Long callerId,
      @PathVariable final DeviationType type,
      @RequestBody @Valid @NotEmpty final List<DisableDeviationRequest> deviations
  ) {

    final List<Workflow> workflows = deviations.stream()
        .map(DisableDeviationRequest::getWorkflow)
        .collect(toList());

    checkUserPermissions(callerId, workflows);

    final List<DisableDeviationInput> deviationsDisable = deviations.stream()
        .map(deviation -> deviation.toDeviationInput(type))
        .collect(toList());

    disableDeviation.execute(logisticCenterId, deviationsDisable);

    return ResponseEntity.noContent().build();
  }

  @InitBinder
  public void initBinder(final PropertyEditorRegistry dataBinder) {
    dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
    dataBinder.registerCustomEditor(ShipmentType.class, new ShipmentTypeEditor());
    dataBinder.registerCustomEditor(DeviationType.class, new DeviationTypeEditor());
  }

    private void checkUserPermissions(final long callerId, final List<Workflow> workflows) {
        final List<UserPermission> permissions = workflows.stream()
                .map(EDIT_PERMISSION_BY_WORKFLOW::get)
                .collect(toList());

        authorizeUser.execute(new AuthorizeUserDto(callerId, permissions));
    }
}
