package com.mercadolibre.planning.model.me.usecases.forecast.parsers;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import com.mercadolibre.planning.model.me.exception.ForecastParsingException;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastColumn;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastSheetDto;
import com.mercadolibre.spreadsheet.MeliDocument;
import com.mercadolibre.spreadsheet.MeliSheet;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ForecastParserHelper {

    private ForecastParserHelper() {}

    public static Map<ForecastColumn, Object> parseSheets(
            final MeliDocument document,
            final Stream<SheetParser> sheetParsers,
            final String warehouseId,
            final LogisticCenterConfiguration config
    ) {
        var parsedSheets = sheetParsers.map(sheetParser -> {
            final String sheetName = sheetParser.name();
            final MeliSheet sheet = document.getSheetByName(sheetName);

            if (sheet == null) {
                throw new ForecastParsingException(
                        format("Sheet name: %s not found in the document.", sheetName)
                );
            }

            return sheetParser.parse(warehouseId, sheet, config);
        })
                .filter(Objects::nonNull)
                .collect(toList());

        return parsedSheets.stream()
                .map(ForecastSheetDto::getValues)
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static String adaptWeekFormat(final String week) {
        String[] arrOfWeek = week.split("-");
        if (arrOfWeek.length == 2) {
            String weekNumber = arrOfWeek[0];
            return format("%s-%s", Integer.valueOf(weekNumber), arrOfWeek[1]);
        }
        return week;
    }
}
