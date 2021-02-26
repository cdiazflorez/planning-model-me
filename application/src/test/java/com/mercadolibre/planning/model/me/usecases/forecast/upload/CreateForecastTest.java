package com.mercadolibre.planning.model.me.usecases.forecast.upload;

import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelForecastGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ForecastCreationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Metadata;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PostForecastResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.dto.ForecastDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastColumnName.WEEK;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateForecastTest {

    @InjectMocks
    private CreateForecast createForecast;

    @Mock
    private PlanningModelForecastGateway planningModelForecastGateway;

    @Test
    void testExecuteOk() {
        // GIVEN
        final ForecastDto input = ForecastDto.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .forecast(mockForecast())
                .build();

        // WHEN
        when(planningModelForecastGateway.postForecast(any(Workflow.class), any(Forecast.class)))
                .thenReturn(new PostForecastResponse(15));

        final ForecastCreationResponse response = createForecast.execute(input);

        // THEN
        verify(planningModelForecastGateway)
                .postForecast(input.getWorkflow(), input.getForecast());
        assertEquals("Considera los días desde: 21/02/2021 hasta: 27/02/2021",
                response.getMessage());

    }

    private Forecast mockForecast() {
        return Forecast.builder()
                .metadata(
                    singletonList(Metadata.builder()
                            .key(WEEK.getName())
                            .value("8-2021")
                            .build()))
                .build();
    }


}
