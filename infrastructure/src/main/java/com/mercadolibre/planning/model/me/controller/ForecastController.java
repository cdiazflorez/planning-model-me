package com.mercadolibre.planning.model.me.controller;

import com.mercadolibre.planning.model.me.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ForecastResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.authorization.AuthorizeUser;
import com.mercadolibre.planning.model.me.usecases.authorization.dtos.AuthorizeUserDto;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.CreateForecast;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.ParseForecastFromFile;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.dto.FileUploadDto;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.dto.ForecastDto;
import com.newrelic.api.agent.Trace;
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

import javax.validation.constraints.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission.OUTBOUND_PROJECTION;
import static org.springframework.http.ResponseEntity.ok;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/planning/model/middleend/workflows/{workflow}/forecasts")
public class ForecastController {

    private final AuthorizeUser authorizeUser;
    private final ParseForecastFromFile parseForecastFromFile;
    private final CreateForecast createForecast;

    @Trace
    @PostMapping("/upload")
    public ResponseEntity<ForecastResponse> upload(
            @PathVariable final Workflow workflow,
            @RequestParam final String warehouseId,
            @RequestParam("caller.id") @NotNull final Long callerId,
            @RequestPart("file") final MultipartFile file) {

        log.info("Uploading forecast. [warehouse_id:{}][workflow:{}][filename:{}][user_id:{}]",
                warehouseId, workflow, file.getOriginalFilename(),callerId);
        authorizeUser.execute(new AuthorizeUserDto(callerId, List.of(OUTBOUND_PROJECTION)));

        final byte[] bytes = getFileBytes(file);

        final Forecast forecast = parseForecastFromFile.execute(
                new FileUploadDto(warehouseId, bytes)
        );
        final ForecastResponse createdForecast = createForecast.execute(ForecastDto.builder()
                .workflow(workflow)
                .forecast(forecast)
                .build()
        );

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
