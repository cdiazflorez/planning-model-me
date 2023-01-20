package com.mercadolibre.planning.model.me.controller.monitor;

import static com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission.OUTBOUND_PROJECTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;

import com.mercadolibre.planning.model.me.controller.RequestClock;
import com.mercadolibre.planning.model.me.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogScheduled;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.ScheduleAdjustment;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.ScheduleBacklogResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.authorization.AuthorizeUser;
import com.mercadolibre.planning.model.me.usecases.authorization.dtos.AuthorizeUserDto;
import com.mercadolibre.planning.model.me.usecases.backlog.GetActiveDeviations;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogScheduled;
import com.mercadolibre.planning.model.me.usecases.monitor.GetMonitor;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.GetMonitorInput;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.Monitor;
import com.newrelic.api.agent.Trace;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/middleend/workflows")
public class MonitorController {

    private final GetMonitor getMonitor;
    private final AuthorizeUser authorizeUser;
    private final GetBacklogScheduled getBacklogScheduled;

    private final GetActiveDeviations getActiveDeviations;

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
    @GetMapping("/fbm-wms-inbound/monitors/new")
    //TODO Interim endpoint migrate this behavior to /fbm-wms-inbound/monitors endpoint.
    public ResponseEntity<ScheduleBacklogResponse> getMonitorInbound(
            @RequestParam("caller.id") @NotNull final Long callerId,
            @RequestParam @NotNull @NotBlank final String logisticCenterId,
            @RequestParam @NotNull final Instant viewDate) {

        authorizeUser.execute(new AuthorizeUserDto(callerId, List.of(OUTBOUND_PROJECTION)));
        final Map<String, BacklogScheduled> backlogScheduledMap = getBacklogScheduled.execute(logisticCenterId, viewDate);
        final List<ScheduleAdjustment> scheduleAdjustments = getActiveDeviations.execute(logisticCenterId, viewDate);
        return ResponseEntity.ok(new ScheduleBacklogResponse(viewDate, scheduleAdjustments, backlogScheduledMap));
    }

    @Trace
    @GetMapping("/fbm-wms-inbound/monitors")
    public ResponseEntity<Map<String, BacklogScheduled>> getMonitorInbound(
        @RequestParam("caller.id") @NotNull final Long callerId,
        @RequestParam @NotNull @NotBlank final String logisticCenterId) {
        authorizeUser.execute(new AuthorizeUserDto(callerId, List.of(OUTBOUND_PROJECTION)));
        return ResponseEntity.ok(getBacklogScheduled.execute(logisticCenterId, requestClock.now()));
    }

    @InitBinder
    public void initBinder(final PropertyEditorRegistry dataBinder) {
        dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
    }
}
