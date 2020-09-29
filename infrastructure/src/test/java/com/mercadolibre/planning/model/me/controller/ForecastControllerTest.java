package com.mercadolibre.planning.model.me.controller;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.CreateForecast;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.ParseForecastFromFile;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.dto.FileUploadDto;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.dto.ForecastDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.charset.StandardCharsets;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.lang.String.format;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ForecastController.class)
public class ForecastControllerTest {

    private static final String URL = "/planning/model/middleend/workflows/%s/forecasts";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ParseForecastFromFile parseForecastFromFile;

    @MockBean
    private CreateForecast createForecast;

    @Test
    void testPostForecastOk() throws Exception {
        // GIVEN
        final byte[] fileContent = "test file content".getBytes(StandardCharsets.UTF_8);
        final MockMultipartFile file = new MockMultipartFile(
                "file",
                "test file.xls",
                MediaType.APPLICATION_JSON_VALUE,
                fileContent
        );
        final FileUploadDto fileUploadDto = new FileUploadDto(WAREHOUSE_ID, fileContent);

        when(parseForecastFromFile.execute(fileUploadDto)).thenReturn(Forecast.builder().build());

        // WHEN
        final ResultActions result = mvc.perform(MockMvcRequestBuilders
                .multipart(format(URL, FBM_WMS_OUTBOUND.getName()) + "/upload")
                .file(file)
                .param("warehouse_id", WAREHOUSE_ID));

        // THEN
        result.andExpect(status().isOk());
        verify(parseForecastFromFile).execute(fileUploadDto);
        verify(createForecast).execute(any(ForecastDto.class));
    }

    @Test
    void testPostForecastMissingParamsThrowsBadRequest() throws Exception {
        // GIVEN
        final byte[] fileContent = "test file content".getBytes(StandardCharsets.UTF_8);
        final MockMultipartFile file = new MockMultipartFile(
                "file",
                "test file.xls",
                MediaType.APPLICATION_JSON_VALUE,
                fileContent
        );

        // WHEN
        final ResultActions result = mvc.perform(MockMvcRequestBuilders
                .multipart(format(URL, FBM_WMS_OUTBOUND.getName()) + "/upload")
                .file(file));

        // THEN
        result.andExpect(status().isBadRequest());
    }

}
