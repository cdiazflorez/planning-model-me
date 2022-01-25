package com.mercadolibre.planning.model.me.controller;

import com.mercadolibre.planning.model.me.controller.editor.ProcessNameEditor;
import com.mercadolibre.planning.model.me.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.metric.DatadogMetricService;
import com.mercadolibre.planning.model.me.usecases.authorization.AuthorizeUser;
import com.mercadolibre.planning.model.me.usecases.authorization.dtos.AuthorizeUserDto;
import com.mercadolibre.planning.model.me.usecases.projection.GetSlaProjection;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetDeferralProjection;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetProjectionInput;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.newrelic.api.agent.Trace;
import lombok.AllArgsConstructor;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission.OUTBOUND_PROJECTION;
import static java.util.Optional.of;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/middleend/workflows/{workflow}")
public class ProjectionController {

    private final AuthorizeUser authorizeUser;
    private final GetSlaProjection getSlaProjection;
    private final GetDeferralProjection getDeferralProjection;
    private final DatadogMetricService datadogMetricService;
    private final RequestClock requestClock;

    private static final Map<Workflow, List<UserPermission>> USER_PERMISSION = Map.of(
            Workflow.FBM_WMS_INBOUND, List.of(OUTBOUND_PROJECTION),
            Workflow.FBM_WMS_OUTBOUND, List.of(OUTBOUND_PROJECTION));

    @Trace
    @GetMapping("/projections/cpt")
    public ResponseEntity<Projection> getCptProjection(
            @PathVariable final Workflow workflow,
            @RequestParam("caller.id") @NotNull final Long callerId,
            @RequestParam final String warehouseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME)
            final ZonedDateTime date) {

        authorizeUser.execute(new AuthorizeUserDto(callerId, USER_PERMISSION.get(workflow)));

        datadogMetricService.trackProjection(warehouseId, workflow, "CPT");

        return ResponseEntity.of(of(getSlaProjection.execute(GetProjectionInputDto.builder()
                .workflow(workflow)
                .warehouseId(warehouseId)
                .date(date)
                .requestDate(requestClock.now())
                .build()))
        );
    }

    @Trace
    @GetMapping("/projections/deferral")
    public ResponseEntity<Projection> getDeferralProjection(
            @PathVariable final Workflow workflow,
            @RequestParam("caller.id") @NotNull final Long callerId,
            @RequestParam final String warehouseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME)
            final ZonedDateTime date,
            @RequestParam(required = false, defaultValue = "false") boolean cap5ToPack) {

        authorizeUser.execute(new AuthorizeUserDto(callerId, USER_PERMISSION.get(workflow)));

        datadogMetricService.trackProjection(warehouseId, workflow, "deferral");

        return ResponseEntity.of(of(getDeferralProjection.execute(new GetProjectionInput(
                warehouseId,
                workflow,
                date,
                null,
                cap5ToPack)))
        );
    }

    @InitBinder
    public void initBinder(final PropertyEditorRegistry dataBinder) {
        dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
        dataBinder.registerCustomEditor(ProcessName.class, new ProcessNameEditor());
    }
}
