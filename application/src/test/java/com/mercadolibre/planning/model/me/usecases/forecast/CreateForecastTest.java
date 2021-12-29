package com.mercadolibre.planning.model.me.usecases.forecast;

import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelForecastGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ForecastCreationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Metadata;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PostForecastResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.WEEK;
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
        final ForecastDto input = new ForecastDto(FBM_WMS_OUTBOUND, mockForecast());

        // WHEN
        when(planningModelForecastGateway.postForecast(any(Workflow.class), any(Forecast.class)))
                .thenReturn(new PostForecastResponse(15));

        final ForecastCreationResponse response = createForecast.execute(input);

        // THEN
        verify(planningModelForecastGateway)
                .postForecast(input.getWorkflow(), input.getForecast());
        assertEquals("Considera los días desde: 26/12/2021 hasta: 01/01/2022",
                response.getMessage());

    }

    private Forecast mockForecast() {
        return Forecast.builder()
                .metadata(
                        singletonList(new Metadata(WEEK.getName(), "1-2022"))
                )
                .build();
    }

}
