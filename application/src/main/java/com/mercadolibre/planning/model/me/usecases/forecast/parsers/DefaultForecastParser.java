package com.mercadolibre.planning.model.me.usecases.forecast.parsers;

import com.mercadolibre.planning.model.me.exception.ForecastParsingException;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastSheetDto;
import com.mercadolibre.spreadsheet.MeliDocument;
import com.mercadolibre.spreadsheet.MeliSheet;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

@Named
public class DefaultForecastParser implements ForecastParser {

    private final Set<SheetParser> sheetParsers;

    @Inject
    public DefaultForecastParser(final Set<SheetParser> sheetParsers) {
        this.sheetParsers = sheetParsers;
    }

    @Override
    public List<ForecastSheetDto> parse(final String warehouseId,
                                        final Workflow workflow,
                                        final MeliDocument document) {
        return sheetParsers.stream()
                .filter(parser -> parser.workflow().equals(workflow))
                .map(sheetParser -> {
                    final String sheetName = sheetParser.name();
                    final MeliSheet sheet = document.getSheetByName(sheetName);

                    if (sheet == null) {
                        throw new ForecastParsingException(
                                format("Sheet name: %s not found in the document.", sheetName)
                        );
                    }

                    return sheetParser.parse(warehouseId, sheet);
                })
                .filter(Objects::nonNull)
                .collect(toList());
    }
}
