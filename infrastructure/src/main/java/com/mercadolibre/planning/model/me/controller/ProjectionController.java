package com.mercadolibre.planning.model.me.controller;

import com.mercadolibre.planning.model.me.controller.editor.ProcessNameEditor;
import com.mercadolibre.planning.model.me.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.me.entities.projection.BacklogProjection;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.metric.DatadogMetricService;
import com.mercadolibre.planning.model.me.usecases.authorization.AuthorizeUser;
import com.mercadolibre.planning.model.me.usecases.authorization.dtos.AuthorizeUserDto;
import com.mercadolibre.planning.model.me.usecases.projection.GetBacklogProjection;
import com.mercadolibre.planning.model.me.usecases.projection.GetCptProjection;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.BacklogProjectionInput;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.newrelic.api.agent.Trace;
import lombok.AllArgsConstructor;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;

import java.util.List;

import static com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission.OUTBOUND_PROJECTION;
import static java.util.Optional.of;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/middleend/workflows/{workflow}")
public class ProjectionController {

    private final AuthorizeUser authorizeUser;
    private final GetCptProjection getCptProjection;
    private final GetBacklogProjection getBacklogProjection;

    private final DatadogMetricService datadogMetricService;

    @Trace
    @GetMapping("/projections/cpt")
    public ResponseEntity<Projection> getCptProjection(
            @PathVariable final Workflow workflow,
            @RequestParam("caller.id") @NotNull final Long callerId,
            @RequestParam final String warehouseId) {

        authorizeUser.execute(new AuthorizeUserDto(callerId, List.of(OUTBOUND_PROJECTION)));

        datadogMetricService.trackProjection(warehouseId, workflow, "CPT");

        return ResponseEntity.of(of(getCptProjection.execute(GetProjectionInputDto.builder()
                .workflow(workflow)
                .warehouseId(warehouseId)
                .build()))
        );
    }

    @Trace
    @GetMapping("/projections/backlog")
    public ResponseEntity<BacklogProjection> getBacklogProjection(
            @PathVariable final Workflow workflow,
            @RequestParam("caller.id") @NotNull final Long callerId,
            final GetBacklogProjectionRequest request) {

        authorizeUser
                .execute(new AuthorizeUserDto(callerId, List.of(OUTBOUND_PROJECTION)));

        datadogMetricService.trackProjection(request.getWarehouseId(),
                workflow,
                "Backlog");

        final BacklogProjectionInput input = request.getBacklogProjectionInput(workflow, callerId);
        return ResponseEntity.of(of(getBacklogProjection.execute(input)));
    }

    @InitBinder
    public void initBinder(final PropertyEditorRegistry dataBinder) {
        dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
        dataBinder.registerCustomEditor(ProcessName.class, new ProcessNameEditor());
    }
}
