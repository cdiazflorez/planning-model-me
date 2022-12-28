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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.WEEK;
import static java.lang.String.format;
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

    @ParameterizedTest
    @CsvSource({
            "53-2022, 25/12/2022, 31/12/2022",
            "1-2023, 01/01/2023, 07/01/2023",
            "2-2023, 08/01/2023, 14/01/2023",
    })
    void testExecuteOk(final String week,
                       final String dateFrom,
                       final String dateTo) {
        // GIVEN
        final ForecastDto input = new ForecastDto(FBM_WMS_OUTBOUND, mockForecast(week));

        // WHEN
        when(planningModelForecastGateway.postForecast(any(Workflow.class), any(Forecast.class)))
                .thenReturn(new PostForecastResponse(15));

        final ForecastCreationResponse response = createForecast.execute(input);

        // THEN
        verify(planningModelForecastGateway).postForecast(input.getWorkflow(), input.getForecast());
        assertEquals(format("Considera los días desde: %s hasta: %s", dateFrom, dateTo), response.getMessage());

    }

    private Forecast mockForecast(final String week) {
        return Forecast.builder()
                .metadata(
                        singletonList(new Metadata(WEEK.getName(), week))
                )
                .build();
    }

}
