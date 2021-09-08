package com.mercadolibre.planning.model.me.controller.backlog;

import com.mercadolibre.planning.model.me.entities.monitor.WorkflowBacklogDetail;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogMonitor;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogMonitorDetails;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorDetailsInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorDetailsResponse;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorInputDto;
import com.mercadolibre.planning.model.me.utils.DateUtils;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static java.time.ZoneOffset.UTC;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

@RestController
@AllArgsConstructor
@RequestMapping("/wms/flow/middleend/logistic_centers/{warehouseId}/backlog")
public class BacklogMonitorController {
    private static final Map<String, String> WORKFLOW_ADAPTER = Map.of(
            FBM_WMS_OUTBOUND.getName(), "outbound-orders"
    );

    private static final long DEFAULT_HOURS_LOOKBACK = 2L;
    private static final long DEFAULT_HOURS_LOOKAHEAD = 8L;

    private final GetBacklogMonitor getBacklogMonitor;

    private final GetBacklogMonitorDetails getBacklogMonitorDetails;

    @GetMapping("/monitor")
    public ResponseEntity<WorkflowBacklogDetail> monitor(
            @PathVariable final String warehouseId,
            @RequestParam final String workflow,
            @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME)
            final ZonedDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME)
            final ZonedDateTime dateTo,
            @RequestParam("caller.id") final long callerId) {

        ZonedDateTime now = DateUtils.getCurrentUtcDateTime().truncatedTo(ChronoUnit.HOURS);

        ZonedDateTime from = dateFrom == null
                ? now.minusHours(DEFAULT_HOURS_LOOKBACK)
                : dateFrom.withZoneSameInstant(UTC);

        ZonedDateTime to = dateTo == null
                ? now.plusHours(DEFAULT_HOURS_LOOKAHEAD) : dateTo.withZoneSameInstant(UTC);

        WorkflowBacklogDetail response = getBacklogMonitor.execute(
                new GetBacklogMonitorInputDto(
                        warehouseId, WORKFLOW_ADAPTER.get(workflow), from, to, callerId
                )
        );

        return ResponseEntity.ok(
                new WorkflowBacklogDetail(
                        workflow,
                        response.getCurrentDatetime(),
                        response.getProcesses()
                )
        );
    }

    @GetMapping("/details")
    public ResponseEntity<GetBacklogMonitorDetailsResponse> details(
            @PathVariable final String warehouseId,
            @RequestParam(required = false) final String workflow,
            @RequestParam final String process,
            @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME)
            final ZonedDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME)
            final ZonedDateTime dateTo,
            @RequestParam("caller.id") final long callerId) {

        ZonedDateTime now = DateUtils.getCurrentUtcDateTime().truncatedTo(ChronoUnit.HOURS);

        ZonedDateTime from = dateFrom == null
                ? now.minusHours(DEFAULT_HOURS_LOOKBACK)
                : dateFrom.withZoneSameInstant(UTC);

        ZonedDateTime to = dateTo == null
                ? now.plusHours(DEFAULT_HOURS_LOOKAHEAD) : dateTo.withZoneSameInstant(UTC);

        return ResponseEntity.ok(
                getBacklogMonitorDetails.execute(
                        GetBacklogMonitorDetailsInput.builder()
                                .warehouseId(warehouseId)
                                .workflow(WORKFLOW_ADAPTER.get(workflow))
                                .process(ProcessName.from(process))
                                .dateFrom(from)
                                .dateTo(to)
                                .callerId(callerId)
                                .build()
                )
        );
    }
}
