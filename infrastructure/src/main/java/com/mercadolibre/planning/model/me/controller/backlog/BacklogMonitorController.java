package com.mercadolibre.planning.model.me.controller.backlog;

import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WAVING;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

import com.mercadolibre.planning.model.me.controller.RequestClock;
import com.mercadolibre.planning.model.me.controller.backlog.exception.NotImplementWorkflowException;
import com.mercadolibre.planning.model.me.entities.monitor.WorkflowBacklogDetail;
import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogMonitor;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogMonitorDetails;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorDetailsInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorDetailsResponse;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorInputDto;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/wms/flow/middleend/logistic_centers/{warehouseId}/backlog")
public class BacklogMonitorController {

  private static final Duration DEFAULT_HOURS_LOOKBACK = Duration.ofHours(2);
  private static final Duration DEFAULT_HOURS_LOOKAHEAD = Duration.ofHours(21);

  private final GetBacklogMonitor getBacklogMonitor;

  private final GetBacklogMonitorDetails getBacklogMonitorDetails;

  private final RequestClock requestClock;

  @GetMapping("/monitor")
  public ResponseEntity<WorkflowBacklogDetail> monitor(
      @PathVariable final String warehouseId,
      @RequestParam final String workflow,
      @RequestParam(required = false) final List<ProcessName> processes,
      @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME) final OffsetDateTime dateFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME) final OffsetDateTime dateTo,
      @RequestParam("caller.id") final long callerId,
      @RequestParam(required = false, defaultValue = "false") final boolean hasWall) {

    var updatedProcesses = processes == null || processes.isEmpty() ? List.of(WAVING, PICKING, PACKING) : processes;

    final Instant requestDate = requestClock.now();
    final Instant startOfCurrentHour = this.getStartHour(dateFrom, requestDate);

    final WorkflowBacklogDetail response = getBacklogMonitor.execute(
        new GetBacklogMonitorInputDto(
            requestDate,
            warehouseId,
            getWorkflow(workflow),
            updatedProcesses,
            dateFrom(dateFrom, startOfCurrentHour),
            dateTo(dateTo, startOfCurrentHour),
            callerId,
            hasWall)
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
      @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME) final OffsetDateTime dateFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME) final OffsetDateTime dateTo,
      @RequestParam("caller.id") final long callerId,
      @RequestParam(required = false, defaultValue = "false") final Boolean hasWall) {

    final Instant requestDate = requestClock.now();
    final Instant startOfCurrentHour = this.getStartHour(dateFrom, requestDate);


    return ResponseEntity.ok(
        getBacklogMonitorDetails.execute(new GetBacklogMonitorDetailsInput(
            requestDate,
            warehouseId,
            getWorkflow(workflow),
            ProcessName.from(process),
            dateFrom(dateFrom, startOfCurrentHour),
            dateTo(dateTo, startOfCurrentHour),
            callerId,
            hasWall)
        )
    );
  }

  private Instant dateFrom(final OffsetDateTime dateFrom, final Instant startOfCurrentHour) {
    return dateFrom == null || dateFrom.toInstant().isBefore(startOfCurrentHour)
        ? startOfCurrentHour.minus(DEFAULT_HOURS_LOOKBACK)
        : dateFrom.toInstant().minus(DEFAULT_HOURS_LOOKBACK);
  }

  private Instant dateTo(final OffsetDateTime dateTo, final Instant startOfCurrentHour) {
    return dateTo == null
        ? startOfCurrentHour.plus(DEFAULT_HOURS_LOOKAHEAD)
        : dateTo.toInstant();
  }

  private Instant getStartHour(final OffsetDateTime dateFrom, final Instant currentDate) {
    final Instant truncatedCurrentDate = currentDate.truncatedTo(ChronoUnit.HOURS);
    return dateFrom == null || dateFrom.toInstant().isBefore(truncatedCurrentDate)
        ? truncatedCurrentDate
        : dateFrom.toInstant().truncatedTo(ChronoUnit.HOURS);
  }

  private Workflow getWorkflow(final String workflowParam) {
    return Workflow.from(workflowParam).orElseThrow(NotImplementWorkflowException::new);
  }
}
