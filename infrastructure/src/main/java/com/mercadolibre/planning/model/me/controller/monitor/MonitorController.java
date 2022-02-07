package com.mercadolibre.planning.model.me.controller.monitor;

import com.mercadolibre.planning.model.me.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.authorization.AuthorizeUser;
import com.mercadolibre.planning.model.me.usecases.authorization.dtos.AuthorizeUserDto;
import com.mercadolibre.planning.model.me.usecases.monitor.GetMonitor;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.GetMonitorInput;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.Monitor;
import com.newrelic.api.agent.Trace;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission.OUTBOUND_PROJECTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/middleend/workflows")
public class MonitorController {

    private final GetMonitor getMonitor;
    private final AuthorizeUser authorizeUser;

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
    public ResponseEntity<Response> getMonitorInbound(
            @RequestParam("caller.id") @NotNull final Long callerId,
            @RequestParam @NotNull @NotBlank final String logisticCenterId){

        authorizeUser.execute(new AuthorizeUserDto(callerId, List.of(OUTBOUND_PROJECTION)));

        return ResponseEntity.of(Optional.of(new Response(
                new Indicator(500, 15500, null),
                new Indicator(350, 10850, null),
                new Indicator(2100, 65100, null),
                new Indicator(150, 4650, 0.3))));
    }

    @InitBinder
    public void initBinder(final PropertyEditorRegistry dataBinder) {
        dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
    }

    @Value
    private static class Response{

        Indicator expected;
        Indicator received;
        Indicator pending;
        Indicator deviation;
    }

    @Value
    private static class Indicator{

        Integer shipments;
        Integer units;
        Double percentage;
    }
}
