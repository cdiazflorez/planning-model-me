package com.mercadolibre.planning.model.me.usecases.forecast.upload;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.HeadcountDistribution;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.HeadcountProductivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Metadata;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistribution;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PolyvalentProductivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingDistribution;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.dto.FileUploadDto;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.dto.ForecastSheetDto;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.parsers.ForecastParser;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastColumnName;
import com.mercadolibre.spreadsheet.MeliDocument;
import lombok.AllArgsConstructor;

import javax.inject.Named;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mercadolibre.planning.model.me.usecases.forecast.upload.utils.SpreadsheetUtils.createMeliDocumentFrom;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastColumnName.HEADCOUNT_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastColumnName.HEADCOUNT_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastColumnName.MONO_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastColumnName.MULTI_BATCH_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastColumnName.MULTI_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastColumnName.PLANNING_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastColumnName.POLYVALENT_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastColumnName.PROCESSING_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastColumnName.WEEK;

@Named
@AllArgsConstructor
public class ParseForecastFromFile implements UseCase<FileUploadDto, Forecast> {

    private final ForecastParser forecastParser;

    @Override
    public Forecast execute(FileUploadDto input) {
        // TODO: Create ForecastParser from input.workflow
        //  so each workflow can have its own Parser.
        final String warehouseId = input.getWarehouseId();
        final MeliDocument document = createMeliDocumentFrom(input.getBytes());
        final List<ForecastSheetDto> parsedSheets = forecastParser.parse(warehouseId, document);

        return createForecastFrom(warehouseId, parsedSheets);
    }

    private static Forecast createForecastFrom(final String warehouseId,
                                               final List<ForecastSheetDto> sheets) {
        final Map<ForecastColumnName, Object> parsedValues = sheets.stream()
                .map(ForecastSheetDto::getValues)
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return Forecast.builder()
                .metadata(buildForecastMetadata(warehouseId, parsedValues))
                .processingDistributions((List<ProcessingDistribution>)
                        parsedValues.get(PROCESSING_DISTRIBUTION))
                .planningDistributions((List<PlanningDistribution>)
                        parsedValues.get(PLANNING_DISTRIBUTION))
                .headcountDistributions((List<HeadcountDistribution>)
                        parsedValues.get(HEADCOUNT_DISTRIBUTION))
                .headcountProductivities((List<HeadcountProductivity>)
                        parsedValues.get(HEADCOUNT_PRODUCTIVITY))
                .polyvalentProductivities((List<PolyvalentProductivity>)
                        parsedValues.get(POLYVALENT_PRODUCTIVITY))
                .build();
    }

    private static List<Metadata> buildForecastMetadata(
            final String warehouseId,
            final Map<ForecastColumnName, Object> parsedValues) {
        return List.of(
                Metadata.builder()
                        .key("warehouse_id")
                        .value(warehouseId)
                        .build(),
                Metadata.builder()
                        .key(WEEK.getName())
                        .value(String.valueOf(parsedValues.get(WEEK)))
                        .build(),
                Metadata.builder()
                        .key(MONO_ORDER_DISTRIBUTION.getName())
                        .value(String.valueOf(parsedValues.get(MONO_ORDER_DISTRIBUTION)))
                        .build(),
                Metadata.builder()
                        .key(MULTI_ORDER_DISTRIBUTION.getName())
                        .value(String.valueOf(parsedValues.get(MULTI_ORDER_DISTRIBUTION)))
                        .build(),
                Metadata.builder()
                        .key(MULTI_BATCH_DISTRIBUTION.getName())
                        .value(String.valueOf(parsedValues.get(MULTI_BATCH_DISTRIBUTION)))
                        .build()
        );
    }

}
