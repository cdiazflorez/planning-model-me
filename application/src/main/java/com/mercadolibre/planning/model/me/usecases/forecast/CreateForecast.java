package com.mercadolibre.planning.model.me.usecases.forecast;

import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelForecastGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ForecastCreationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Metadata;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PostForecastResponse;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastDto;
import lombok.RequiredArgsConstructor;

import javax.inject.Named;

import java.util.List;

import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.WEEK;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getDatesBetween;
import static java.lang.String.format;
import static java.util.Objects.nonNull;

@Named
@RequiredArgsConstructor
public class CreateForecast {

    private final PlanningModelForecastGateway planningModelForecastGateway;

    public ForecastCreationResponse execute(final ForecastDto input) {
        final PostForecastResponse postForecast =
                planningModelForecastGateway.postForecast(input.getWorkflow(), input.getForecast());
        return buildResponse(input.getForecast().getMetadata(), nonNull(postForecast));
    }

    private ForecastCreationResponse buildResponse(final List<Metadata> metadataList,
                                                   final boolean isValid) {

        String response = "";
        if (isValid) {
            response = metadataList.stream()
                    .filter(metadata -> metadata.getKey().equals(WEEK.getName()))
                    .findFirst()
                    .map(metadata -> {
                        final var weekOfYear = metadata.getValue().split("-");
                        final String[] datesBetween = getDatesBetween(weekOfYear);
                        return format("Considera los días desde: %1$s hasta: %2$s",
                                          datesBetween[0], datesBetween[1]);
                    })
                    .orElse("");
        }
        return new ForecastCreationResponse(response);
    }
}
