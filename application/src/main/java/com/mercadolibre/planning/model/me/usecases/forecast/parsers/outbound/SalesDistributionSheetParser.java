package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound;

import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Metadata;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistribution;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastSheetDto;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.SheetParser;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName;
import com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils;
import com.mercadolibre.planning.model.me.utils.TestLogisticCenterMapper;
import com.mercadolibre.spreadsheet.MeliCell;
import com.mercadolibre.spreadsheet.MeliRow;
import com.mercadolibre.spreadsheet.MeliSheet;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.Range;

import javax.inject.Named;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.PLANNING_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getIntValueAt;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getStringValueAt;
import static java.util.stream.Collectors.toList;

@Named
@AllArgsConstructor
public class SalesDistributionSheetParser implements SheetParser {

    private static final Range<Integer> COLUMN_RANGE = Range.between(1, 6);
    private LogisticCenterGateway logisticCenterGateway;

    @Override
    public String name() {
        return "Distribucion ventas";
    }

    @Override
    public Workflow workflow() {
        return FBM_WMS_OUTBOUND;
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
                .map(row -> createPlanningDistributionFrom(warehouseId, row, sheet))
                .collect(toList());
    }

    private boolean rowIsNotEmpty(final MeliRow row) {
        return row.getCells().stream()
                .filter(meliCell -> COLUMN_RANGE.contains(meliCell.getColumnIndex()))
                .map(MeliCell::getValue)
                .filter(Objects::nonNull)
                .anyMatch(value -> !value.isEmpty());
    }

    private PlanningDistribution createPlanningDistributionFrom(final String warehouseId,
                                                                final MeliRow row,
                                                                final MeliSheet sheet) {

        final LogisticCenterConfiguration configuration = logisticCenterGateway.getConfiguration(
                TestLogisticCenterMapper.toRealLogisticCenter(warehouseId));

        final ZoneId zoneId = configuration.getZoneId();

        final String carrierId = getStringValueAt(sheet,row, 3);
        final String serviceId = String.valueOf(getIntValueAt(sheet, row, 4));
        final String canalization = getStringValueAt(sheet,row, 5);

        return PlanningDistribution.builder()
                .dateOut(SpreadsheetUtils.getDateTimeAt(sheet, row, 1, zoneId))
                .dateIn(SpreadsheetUtils.getDateTimeAt(sheet, row, 2, zoneId))
                .metadata(List.of(
                        new Metadata(ForecastColumnName.CARRIER_ID.name(), carrierId),
                        new Metadata(ForecastColumnName.SERVICE_ID.name(), serviceId),
                        new Metadata(ForecastColumnName.CANALIZATION.name(), canalization)
                ))
                .quantity(getIntValueAt(sheet, row,6))
                .quantityMetricUnit(MetricUnit.UNITS.getName())
                .build();
    }
}