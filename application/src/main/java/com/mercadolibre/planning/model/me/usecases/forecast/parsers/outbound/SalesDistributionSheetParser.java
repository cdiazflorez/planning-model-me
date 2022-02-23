package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound;

import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Metadata;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistribution;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastSheetDto;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.SheetParser;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName;
import com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils;
import com.mercadolibre.spreadsheet.MeliCell;
import com.mercadolibre.spreadsheet.MeliRow;
import com.mercadolibre.spreadsheet.MeliSheet;
import org.apache.commons.lang3.Range;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.PLANNING_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getIntValueAt;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getStringValueAt;
import static java.util.stream.Collectors.toList;

public class SalesDistributionSheetParser implements SheetParser {

    private static final Range<Integer> COLUMN_RANGE = Range.between(1, 6);
    private static final int CARRIER_ID_COLUMN = 3;
    private static final int SERVICE_ID_COLUMN = 4;
    private static final int CANALIZATION_COLUMN = 5;
    private static final int PLANNING_DISTRIBUTION_STARTING_ROW = 3;
    private static final int PD_DATE_OUT_ROW = 1;
    private static final int PD_DATE_IN_ROW = 2;
    private static final int PD_QUANTITY_ROW = 6;

    @Override
    public String name() {
        return "Distribucion ventas";
    }

    @Override
    public ForecastSheetDto parse(
            final String warehouseId,
            final MeliSheet sheet,
            final LogisticCenterConfiguration config
    ) {
        return new ForecastSheetDto(
                sheet.getSheetName(),
                Map.of(PLANNING_DISTRIBUTION, parsePlanningDistributionRows(sheet, config))
        );
    }

    private List<PlanningDistribution> parsePlanningDistributionRows(
            final MeliSheet sheet,
            final LogisticCenterConfiguration config
    ) {
        return sheet.getRowsStartingFrom(PLANNING_DISTRIBUTION_STARTING_ROW).stream()
                .filter(this::rowIsNotEmpty)
                .map(row -> createPlanningDistributionFrom(row, sheet, config))
                .collect(toList());
    }

    private boolean rowIsNotEmpty(final MeliRow row) {
        return row.getCells().stream()
                .filter(meliCell -> COLUMN_RANGE.contains(meliCell.getColumnIndex()))
                .map(MeliCell::getValue)
                .filter(Objects::nonNull)
                .anyMatch(value -> !value.isEmpty());
    }

    private PlanningDistribution createPlanningDistributionFrom(
            final MeliRow row,
            final MeliSheet sheet,
            final LogisticCenterConfiguration config
    ) {
        final ZoneId zoneId = config.getZoneId();

        final String carrierId = getStringValueAt(sheet, row, CARRIER_ID_COLUMN);
        final String serviceId = String.valueOf(getIntValueAt(sheet, row, SERVICE_ID_COLUMN));
        final String canalization = getStringValueAt(sheet, row, CANALIZATION_COLUMN);

        return PlanningDistribution.builder()
                .dateOut(SpreadsheetUtils.getDateTimeAt(sheet, row, PD_DATE_OUT_ROW, zoneId))
                .dateIn(SpreadsheetUtils.getDateTimeAt(sheet, row, PD_DATE_IN_ROW, zoneId))
                .metadata(List.of(
                        new Metadata(ForecastColumnName.CARRIER_ID.name(), carrierId),
                        new Metadata(ForecastColumnName.SERVICE_ID.name(), serviceId),
                        new Metadata(ForecastColumnName.CANALIZATION.name(), canalization)
                ))
                .quantity(getIntValueAt(sheet, row, PD_QUANTITY_ROW))
                .quantityMetricUnit(MetricUnit.UNITS.getName())
                .build();
    }
}
