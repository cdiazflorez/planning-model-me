package com.mercadolibre.planning.model.me.usecases.forecast.upload;

import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelForecastGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.dto.ForecastDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

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
                .workflow(Workflow.FBM_WMS_OUTBOUND)
                .forecast(Forecast.builder().build())
                .build();

        // WHEN
        createForecast.execute(input);

        // THEN
        verify(planningModelForecastGateway)
                .postForecast(input.getWorkflow(), input.getForecast());
    }


}
