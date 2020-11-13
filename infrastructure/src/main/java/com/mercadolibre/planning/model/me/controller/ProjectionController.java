package com.mercadolibre.planning.model.me.controller;

import com.mercadolibre.planning.model.me.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.projection.GetForecastProjection;
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

import java.util.Optional;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/middleend/workflows/{workflow}")
public class ProjectionController {

    private final GetForecastProjection getProjection;

    @Trace
    @GetMapping("/projections")
    public ResponseEntity<Projection> getProjection(
            @PathVariable final Workflow workflow,
            @RequestParam("warehouse_id") final String warehouseId) {

        return ResponseEntity.of(Optional.of(getProjection.execute(GetProjectionInputDto.builder()
                .workflow(workflow)
                .warehouseId(warehouseId)
                .build()))
        );
    }

    @InitBinder
    public void initBinder(final PropertyEditorRegistry dataBinder) {
        dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
    }
}
