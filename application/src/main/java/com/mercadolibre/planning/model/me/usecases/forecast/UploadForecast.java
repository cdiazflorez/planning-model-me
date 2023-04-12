package com.mercadolibre.planning.model.me.usecases.forecast;

import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.createMeliDocumentFrom;

import com.mercadolibre.planning.model.me.enums.ProcessPath;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ForecastCreationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastDto;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.ForecastParser;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.ProcessPathValidator;
import com.mercadolibre.spreadsheet.MeliDocument;
import java.util.Set;
import java.util.function.Consumer;
import javax.inject.Named;
import lombok.RequiredArgsConstructor;

@Named
@RequiredArgsConstructor
public class UploadForecast {
  private final CreateForecast createForecast;

  private final LogisticCenterGateway logisticCenterGateway;

  private final ProcessPathGateway processPathGateway;

  public ForecastCreationResponse upload(final String warehouseId,
                                         final Workflow workflow,
                                         final ForecastParser forecastParser,
                                         final byte[] bytes,
                                         final Long callerId) {

    final MeliDocument document = createMeliDocumentFrom(bytes);

    final var config = logisticCenterGateway.getConfiguration(warehouseId);

    final var forecast = forecastParser.parse(warehouseId, document, callerId, config);

    final var validators = createValidators(warehouseId, workflow);

    validators.forEach(validator -> validator.accept(forecast));

    return createForecast.execute(new ForecastDto(workflow, forecast));
  }

  private Set<Consumer<Forecast>> createValidators(final String warehouseId, final Workflow workflow) {
    final var processPaths = processPathGateway.getProcessPathGateway(warehouseId);
    final var processPathValidator = new ProcessPathValidator(processPaths, workflow);

    return Set.of(processPathValidator);
  }

  /**
   * Process paths gateway.
   * Obtain a set of pp per logistic Center
   */
  public interface ProcessPathGateway {
    Set<ProcessPath> getProcessPathGateway(String logisticCenterId);
  }
}
