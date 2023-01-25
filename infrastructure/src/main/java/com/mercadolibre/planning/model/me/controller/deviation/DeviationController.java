package com.mercadolibre.planning.model.me.controller.deviation;

import static com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission.OUTBOUND_SIMULATION;
import static java.util.Collections.singletonList;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

import com.mercadolibre.planning.model.me.controller.deviation.request.DeviationRequest;
import com.mercadolibre.planning.model.me.controller.editor.DeviationTypeEditor;
import com.mercadolibre.planning.model.me.controller.editor.ShipmentTypeEditor;
import com.mercadolibre.planning.model.me.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.me.enums.DeviationType;
import com.mercadolibre.planning.model.me.enums.ShipmentType;
import com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.DeviationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.metric.DatadogMetricService;
import com.mercadolibre.planning.model.me.usecases.authorization.AuthorizeUser;
import com.mercadolibre.planning.model.me.usecases.authorization.dtos.AuthorizeUserDto;
import com.mercadolibre.planning.model.me.usecases.deviation.DisableDeviation;
import com.mercadolibre.planning.model.me.usecases.deviation.SaveDeviation;
import com.mercadolibre.planning.model.me.usecases.deviation.SaveOutboundDeviation;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.DisableDeviationInput;
import com.newrelic.api.agent.Trace;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/planning/model/middleend/workflows/{workflow}/deviations")
public class DeviationController {

  // TODO: Update Permissions when inbound-edition adoption improves
  private static final Map<Workflow, UserPermission> EDIT_PERMISSION_BY_WORKFLOW = Map.of(
      Workflow.FBM_WMS_OUTBOUND, OUTBOUND_SIMULATION,
      Workflow.FBM_WMS_INBOUND, OUTBOUND_SIMULATION,
      Workflow.INBOUND, OUTBOUND_SIMULATION,
      Workflow.INBOUND_TRANSFER, OUTBOUND_SIMULATION
  );

  private final SaveOutboundDeviation saveOutboundDeviation;

  private final SaveDeviation saveDeviation;

  private final DisableDeviation disableDeviation;

  private final AuthorizeUser authorizeUser;

  private final DatadogMetricService datadogMetricService;

  @Trace
  @PostMapping("/save")
  public ResponseEntity<DeviationResponse> save(
      @PathVariable final Workflow workflow,
      @RequestBody @Valid final DeviationRequest deviationRequest) {

    authorizeUser.execute(new AuthorizeUserDto(
        deviationRequest.getUserId(), singletonList(OUTBOUND_SIMULATION)));

    datadogMetricService.trackDeviationAdjustment(deviationRequest);

    DeviationResponse response;

    try {
      response = saveOutboundDeviation.execute(deviationRequest.toDeviationInput(workflow, DeviationType.UNITS));
    } catch (Exception e) {
      response = new DeviationResponse(INTERNAL_SERVER_ERROR.value(),
          "Error persisting forecast deviation");
    }

    return ResponseEntity.status(response.getStatus())
        .body(response);
  }

  @Trace
  @PostMapping("/{type}/save")
  public ResponseEntity<HttpStatus> save(
      @PathVariable final DeviationType type,
      @PathVariable final Workflow workflow,
      @RequestBody @Valid final DeviationRequest deviationRequest) {

    datadogMetricService.trackDeviationAdjustment(deviationRequest);

    saveDeviation.execute(deviationRequest.toDeviationInput(workflow, type));

    return new ResponseEntity<>(OK);
  }

  @Trace
  @PostMapping("/disable")
  public ResponseEntity<Void> disable(
      @PathVariable final Workflow workflow,
      @RequestParam final String warehouseId,
      @RequestParam("caller.id") @NotNull final Long callerId
  ) {

    authorizeUser.execute(new AuthorizeUserDto(callerId, singletonList(OUTBOUND_SIMULATION)));

    disableDeviation.execute(new DisableDeviationInput(warehouseId, workflow));
    return ResponseEntity.status(OK).build();
  }

  @Trace
  @PostMapping("/{type}/disable")
  public ResponseEntity<Void> disable(
      @RequestParam final String logisticCenterId,
      @PathVariable final Workflow workflow,
      @PathVariable final DeviationType type,
      @RequestParam("caller.id") @NotNull final Long callerId
  ) {
    final var permission = EDIT_PERMISSION_BY_WORKFLOW.get(workflow);
    authorizeUser.execute(new AuthorizeUserDto(callerId, singletonList(permission)));

    disableDeviation.execute(new DisableDeviationInput(logisticCenterId, workflow));
    return ResponseEntity.status(OK).build();
  }

  @InitBinder
  public void initBinder(final PropertyEditorRegistry dataBinder) {
    dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
    //TODO: you need to add the list as a parameter in the deviation request;
    dataBinder.registerCustomEditor(ShipmentType.class, new ShipmentTypeEditor());
    dataBinder.registerCustomEditor(DeviationType.class, new DeviationTypeEditor());
  }
}
