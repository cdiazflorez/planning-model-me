package com.mercadolibre.planning.model.me.controller.deviation;

import static com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission.OUTBOUND_SIMULATION;
import static java.util.Collections.singletonList;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

import com.mercadolibre.planning.model.me.controller.deviation.request.DeviationRequest;
import com.mercadolibre.planning.model.me.controller.editor.DeviationTypeEditor;
import com.mercadolibre.planning.model.me.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.me.enums.DeviationType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.DeviationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.metric.DatadogMetricService;
import com.mercadolibre.planning.model.me.usecases.authorization.AuthorizeUser;
import com.mercadolibre.planning.model.me.usecases.authorization.dtos.AuthorizeUserDto;
import com.mercadolibre.planning.model.me.usecases.deviation.DisableDeviation;
import com.mercadolibre.planning.model.me.usecases.deviation.SaveDeviation;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.DisableDeviationInput;
import com.newrelic.api.agent.Trace;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.PropertyEditorRegistry;
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
            response = saveDeviation.execute(deviationRequest.toDeviationInput(workflow));
        } catch (Exception e) {
            response = new DeviationResponse(INTERNAL_SERVER_ERROR.value(),
                    "Error persisting forecast deviation");
        }

        return ResponseEntity.status(response.getStatus())
                .body(response);
    }

    @Trace
    @PostMapping("/{type}/save")
    public ResponseEntity<DeviationResponse> save(
        @PathVariable final DeviationType type,
        @RequestBody @Valid final DeviationRequest deviationRequest) {

        datadogMetricService.trackDeviationAdjustment(deviationRequest);

        DeviationResponse response = new DeviationResponse(OK.value(), "Schedule deviation saved");

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Trace
    @PostMapping("/disable")
    public ResponseEntity<DeviationResponse> disable(
            @PathVariable final Workflow workflow,
            @RequestParam final String warehouseId,
            @RequestParam("caller.id") @NotNull final Long callerId) {

        authorizeUser.execute(new AuthorizeUserDto(callerId, singletonList(OUTBOUND_SIMULATION)));

        DeviationResponse response;

        try {
            response = disableDeviation.execute(new DisableDeviationInput(warehouseId, workflow));
        } catch (Exception e) {
            response = new DeviationResponse(INTERNAL_SERVER_ERROR.value(),
                     "Error disabling forecast deviation");
        }

        return ResponseEntity.status(response.getStatus())
                .body(response);
    }

    @InitBinder
    public void initBinder(final PropertyEditorRegistry dataBinder) {
        dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
        dataBinder.registerCustomEditor(DeviationType.class, new DeviationTypeEditor());
    }
}
