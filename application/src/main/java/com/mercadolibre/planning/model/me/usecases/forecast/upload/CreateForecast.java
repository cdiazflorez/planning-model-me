package com.mercadolibre.planning.model.me.usecases.forecast.upload;

import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelForecastGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ForecastCreationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Metadata;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PostForecastResponse;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.dto.ForecastDto;
import lombok.AllArgsConstructor;

import javax.inject.Named;

import java.util.List;

import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastColumnName.WEEK;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getDatesBetween;
import static java.lang.String.format;
import static java.util.Objects.nonNull;

@Named
@AllArgsConstructor
public class CreateForecast implements UseCase<ForecastDto, ForecastCreationResponse> {

    private final PlanningModelForecastGateway planningModelForecastGateway;

    @Override
    public ForecastCreationResponse execute(final ForecastDto input) {
        final PostForecastResponse postForecast =
                planningModelForecastGateway.postForecast(input.getWorkflow(), input.getForecast());
        return buildResponse(input.getForecast().getMetadata(), nonNull(postForecast));
    }

    private ForecastCreationResponse buildResponse(final List<Metadata> metadataList,
                                                   final boolean isValid) {

        String response = "";

        if (isValid) {
            final String[] weekOfYear = metadataList.stream()
                    .filter(metadata -> metadata.getKey().equals(WEEK.getName()))
                    .findFirst().get().getValue().split("-");
            final String[] datesBetween = getDatesBetween(weekOfYear);
            response = format("Considera los días desde: %1$s hasta: %2$s",
                    datesBetween[0], datesBetween[1]);
        }
        return new ForecastCreationResponse(response);
    }
}
