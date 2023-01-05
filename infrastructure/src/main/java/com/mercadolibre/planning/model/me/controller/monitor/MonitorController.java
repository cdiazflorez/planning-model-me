package com.mercadolibre.planning.model.me.controller.monitor;

import com.mercadolibre.planning.model.me.controller.RequestClock;
import com.mercadolibre.planning.model.me.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogScheduled;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.authorization.AuthorizeUser;
import com.mercadolibre.planning.model.me.usecases.authorization.dtos.AuthorizeUserDto;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogScheduled;
import com.mercadolibre.planning.model.me.usecases.monitor.GetMonitor;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.GetMonitorInput;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.Monitor;
import com.newrelic.api.agent.Trace;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission.OUTBOUND_PROJECTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/middleend/workflows")
public class MonitorController {

    private final GetMonitor getMonitor;
    private final AuthorizeUser authorizeUser;
    private final GetBacklogScheduled getBacklogScheduled;
    private final RequestClock requestClock;


    @Trace
    @GetMapping("/fbm-wms-outbound/monitors")
    public ResponseEntity<Monitor> getMonitorOutbound(
            @RequestParam("caller.id") @NotNull final Long callerId,
            @RequestParam @NotNull @NotBlank final String warehouseId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            final ZonedDateTime dateFrom,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            final ZonedDateTime dateTo) {

        authorizeUser.execute(new AuthorizeUserDto(callerId, List.of(OUTBOUND_PROJECTION)));

        return ResponseEntity.of(Optional.of(getMonitor.execute(
                        GetMonitorInput.builder()
                                .workflow(FBM_WMS_OUTBOUND)
                                .warehouseId(warehouseId)
                                .dateFrom(dateFrom)
                                .dateTo(dateTo)
                                .build()
                ))
        );
    }

    @Trace
    @GetMapping("/fbm-wms-inbound/monitors")
    public ResponseEntity<Map<String, BacklogScheduled>> getMonitorInbound(
            @RequestParam("caller.id") @NotNull final Long callerId,
            @RequestParam @NotNull @NotBlank final String logisticCenterId){

        authorizeUser.execute(new AuthorizeUserDto(callerId, List.of(OUTBOUND_PROJECTION)));
        return ResponseEntity.ok(getBacklogScheduled.execute(logisticCenterId, requestClock.now()));
    }

    @InitBinder
    public void initBinder(final PropertyEditorRegistry dataBinder) {
        dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
    }

}
