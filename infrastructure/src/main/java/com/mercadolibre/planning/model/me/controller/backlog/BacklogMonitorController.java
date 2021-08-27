package com.mercadolibre.planning.model.me.controller.backlog;

import com.mercadolibre.planning.model.me.entities.monitor.WorkflowBacklogDetail;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogMonitor;
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

import static java.time.ZoneOffset.UTC;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

@RestController
@AllArgsConstructor
@RequestMapping("/wms/flow/middleend/logistic_centers/{warehouseId}/backlog")
public class BacklogMonitorController {

    private static final long DEFAULT_HOURS_LOOKBACK = 2L;
    private static final long DEFAULT_HOURS_LOOKAHEAD = 22L;

    private final GetBacklogMonitor getBacklogMonitor;

    @GetMapping("/monitor")
    public ResponseEntity<WorkflowBacklogDetail> monitor(
            @PathVariable final String warehouseId,
            @RequestParam final String workflow,
            @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME)
            final ZonedDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME)
            final ZonedDateTime dateTo,
            @RequestParam("caller.id") final long callerId) {

        ZonedDateTime now = DateUtils.getCurrentUtcDateTime();

        ZonedDateTime from = dateFrom == null
                ? now.minusHours(DEFAULT_HOURS_LOOKBACK) : dateFrom.withZoneSameInstant(UTC);

        ZonedDateTime to = dateTo == null
                ? now.plusHours(DEFAULT_HOURS_LOOKAHEAD) : dateTo.withZoneSameInstant(UTC);

        return ResponseEntity.ok(
                getBacklogMonitor.execute(
                        new GetBacklogMonitorInputDto(warehouseId, workflow, from, to, callerId)
                )
        );
    }
}
