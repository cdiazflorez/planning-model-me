package com.mercadolibre.planning.model.me.controller.backlog;

import com.mercadolibre.planning.model.me.config.FeatureToggle;
import com.mercadolibre.planning.model.me.controller.RequestClock;
import com.mercadolibre.planning.model.me.controller.backlog.exception.EmptyStateException;
import com.mercadolibre.planning.model.me.controller.backlog.exception.NotImplementWorkflowException;
import com.mercadolibre.planning.model.me.entities.monitor.WorkflowBacklogDetail;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogMonitor;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogMonitorDetails;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorDetailsInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorDetailsResponse;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorInputDto;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

@RestController
@AllArgsConstructor
@RequestMapping("/wms/flow/middleend/logistic_centers/{warehouseId}/backlog")
public class BacklogMonitorController {

    private static final Duration DEFAULT_HOURS_LOOKBACK = Duration.ofHours(2);
    private static final Duration DEFAULT_HOURS_LOOKAHEAD = Duration.ofHours(8);

    private final GetBacklogMonitor getBacklogMonitor;

    private final GetBacklogMonitorDetails getBacklogMonitorDetails;

    private final FeatureToggle featureToggle;

    private final RequestClock requestClock;

    @GetMapping("/monitor")
    public ResponseEntity<WorkflowBacklogDetail> monitor(
            @PathVariable final String warehouseId,
            @RequestParam final String workflow,
            @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME)
            final OffsetDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME)
            final OffsetDateTime dateTo,
            @RequestParam("caller.id") final long callerId) {

        if (!featureToggle.hasBacklogMonitorFeatureEnabled(warehouseId)) {
            throw new EmptyStateException();
        }

        final Instant requestDate = requestClock.now();
        final Instant startOfCurrentHour = requestDate.truncatedTo(ChronoUnit.HOURS);

        final WorkflowBacklogDetail response = getBacklogMonitor.execute(
                new GetBacklogMonitorInputDto(
                        requestDate,
                        warehouseId,
                        getWorkflow(workflow),
                        dateFrom(dateFrom, startOfCurrentHour),
                        dateTo(dateTo, startOfCurrentHour),
                        callerId)
        );

        return ResponseEntity.ok(
                new WorkflowBacklogDetail(
                        getWorkflow(workflow).getName(),
                        response.getCurrentDatetime(),
                        response.getProcesses())
        );
    }

    @GetMapping("/details")
    public ResponseEntity<GetBacklogMonitorDetailsResponse> details(
            @PathVariable final String warehouseId,
            @RequestParam final String workflow,
            @RequestParam final String process,
            @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME)
            final OffsetDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME)
            final OffsetDateTime dateTo,
            @RequestParam("caller.id") final long callerId) {

        if (!featureToggle.hasBacklogMonitorFeatureEnabled(warehouseId)) {
            throw new EmptyStateException();
        }

        final Instant requestDate = requestClock.now();
        final Instant startOfCurrentHour = requestDate.truncatedTo(ChronoUnit.HOURS);

        return ResponseEntity.ok(
                getBacklogMonitorDetails.execute(new GetBacklogMonitorDetailsInput(
                        requestDate,
                        warehouseId,
                        getWorkflow(workflow),
                        ProcessName.from(process),
                        dateFrom(dateFrom, startOfCurrentHour),
                        dateTo(dateTo, startOfCurrentHour),
                        callerId
                ))
        );
    }

    private Instant dateFrom(final OffsetDateTime date, final Instant startOfCurrentHour) {
        return date == null
                ? startOfCurrentHour.minus(DEFAULT_HOURS_LOOKBACK)
                : date.toInstant();
    }

    private Instant dateTo(final OffsetDateTime date, final Instant startOfCurrentHour) {
        return date == null
                ? startOfCurrentHour.plus(DEFAULT_HOURS_LOOKAHEAD)
                : date.toInstant();
    }

    private Workflow getWorkflow(final String workflowParam) {
        return Workflow.from(workflowParam).orElseThrow(NotImplementWorkflowException::new);
    }
}
