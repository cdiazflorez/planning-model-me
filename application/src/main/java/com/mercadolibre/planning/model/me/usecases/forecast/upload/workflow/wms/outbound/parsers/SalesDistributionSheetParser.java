package com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.parsers;

import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Metadata;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistribution;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.dto.ForecastSheetDto;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.parsers.SheetParser;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastColumnName;
import com.mercadolibre.spreadsheet.MeliCell;
import com.mercadolibre.spreadsheet.MeliRow;
import com.mercadolibre.spreadsheet.MeliSheet;
import lombok.AllArgsConstructor;

import javax.inject.Named;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.mercadolibre.planning.model.me.usecases.forecast.upload.utils.SpreadsheetUtils.formatter;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.utils.SpreadsheetUtils.getBigIntValueAt;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.utils.SpreadsheetUtils.getIntValueAt;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.utils.SpreadsheetUtils.getValueAt;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastColumnName.PLANNING_DISTRIBUTION;
import static java.util.stream.Collectors.toList;

@Named
@AllArgsConstructor
public class SalesDistributionSheetParser implements SheetParser {

    private LogisticCenterGateway logisticCenterGateway;

    @Override
    public String name() {
        return "Distribucion ventas";
    }

    @Override
    public ForecastSheetDto parse(final String warehouseId, final MeliSheet sheet) {
        return new ForecastSheetDto(
                sheet.getSheetName(),
                Map.of(PLANNING_DISTRIBUTION, parsePlanningDistributionRows(sheet, 3, warehouseId))
        );
    }

    private List<PlanningDistribution> parsePlanningDistributionRows(final MeliSheet sheet,
                                                                     final int startingRow,
                                                                     final String warehouseId) {
        return sheet.getRowsStartingFrom(startingRow).stream()
                .filter(this::rowIsNotEmpty)
                .map((row) -> createPlanningDistributionFrom(warehouseId, row))
                .collect(toList());
    }

    private boolean rowIsNotEmpty(final MeliRow row) {
        return row.getCells().stream()
                .map(MeliCell::getValue)
                .filter(Objects::nonNull)
                .anyMatch(value -> !value.isEmpty());
    }

    private PlanningDistribution createPlanningDistributionFrom(final String warehouseId,
                                                                final MeliRow row) {
        final LogisticCenterConfiguration configuration =
                logisticCenterGateway.getConfiguration(warehouseId);

        return PlanningDistribution.builder()
                .dateOut(ZonedDateTime
                        .parse(getValueAt(row, 1),
                                formatter.withZone(configuration.getTimeZone().toZoneId()))
                )
                .dateIn(ZonedDateTime
                        .parse(getValueAt(row, 2),
                                formatter.withZone(configuration.getTimeZone().toZoneId()))
                )
                .metadata(List.of(
                        Metadata.builder()
                                .key(ForecastColumnName.CARRIER_ID.name())
                                .value(String.valueOf(getBigIntValueAt(row, 3)))
                                .build(),
                        Metadata.builder()
                                .key(ForecastColumnName.SERVICE_ID.name())
                                .value(String.valueOf(getIntValueAt(row, 4)))
                                .build(),
                        Metadata.builder()
                                .key(ForecastColumnName.CANALIZATION.name())
                                .value(getValueAt(row, 5))
                                .build()
                ))
                .quantity(getIntValueAt(row,6))
                .quantityMetricUnit(MetricUnit.UNITS.getName())
                .build();
    }
}