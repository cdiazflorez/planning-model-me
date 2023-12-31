package com.mercadolibre.planning.model.me.controller;

import static com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission.OUTBOUND_FORECAST;
import static org.springframework.http.ResponseEntity.ok;

import com.mercadolibre.planning.model.me.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ForecastCreationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.metric.DatadogMetricService;
import com.mercadolibre.planning.model.me.usecases.authorization.AuthorizeUser;
import com.mercadolibre.planning.model.me.usecases.authorization.dtos.AuthorizeUserDto;
import com.mercadolibre.planning.model.me.usecases.forecast.UploadForecast;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.Target;
import com.newrelic.api.agent.Trace;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/planning/model/middleend/workflows/{workflow}/forecasts")
public class ForecastController {

    private final AuthorizeUser authorizeUser;
    private final UploadForecast uploadForecast;
    private final DatadogMetricService datadogMetricService;

    @Trace
    @PostMapping("/upload")
    public ResponseEntity<ForecastCreationResponse> upload(
            @PathVariable final Workflow workflow,
            @RequestParam final String warehouseId,
            @RequestParam("caller.id") @NotNull final Long callerId,
            @RequestPart("file") final MultipartFile file) {

        log.info("Uploading forecast. [warehouse_id:{}][workflow:{}][filename:{}][user_id:{}]",
                warehouseId, workflow, file.getOriginalFilename(), callerId);

        authorizeUser.execute(new AuthorizeUserDto(callerId, List.of(OUTBOUND_FORECAST)));

        final byte[] bytes = getFileBytes(file);

        final ForecastCreationResponse createdForecast = uploadForecast.upload(
                warehouseId, workflow, Target.from(workflow).forecastParser, bytes, callerId
        );

        datadogMetricService.trackForecastUpload(warehouseId);

        return ok(createdForecast);
    }

    @InitBinder
    public void initBinder(final PropertyEditorRegistry dataBinder) {
        dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
    }

    private byte[] getFileBytes(MultipartFile file) {
        try {
            final InputStream inputStream = file.getInputStream();
            final byte[] bytes = inputStream.readAllBytes();
            inputStream.close();

            return bytes;
        } catch (final IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unable to open document. File might be corrupt.");
        }
    }
}
