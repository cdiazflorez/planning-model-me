package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound;

import static com.mercadolibre.planning.model.me.enums.ProcessPath.GLOBAL;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.PLANNING_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessPathConfiguration.CANALIZATION_COLUMN;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessPathConfiguration.CARRIER_ID_COLUMN;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessPathConfiguration.PD_DATE_IN_COLUMN;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessPathConfiguration.PD_DATE_OUT_COLUMN;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessPathConfiguration.PD_QUANTITY_COLUMN;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessPathConfiguration.PROCESS_PATH_ID_COLUMN;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessPathConfiguration.SERVICE_ID_COLUMN;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getDoubleValueAt;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getIntValueAt;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getStringValueAt;
import static java.util.stream.Collectors.toList;

import com.mercadolibre.planning.model.me.enums.ProcessPath;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistribution;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastSheetDto;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.SheetParser;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessPathConfiguration;
import com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils;
import com.mercadolibre.spreadsheet.MeliCell;
import com.mercadolibre.spreadsheet.MeliRow;
import com.mercadolibre.spreadsheet.MeliSheet;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Setter;
import org.apache.commons.lang3.Range;
import org.apache.commons.math3.util.Precision;

@Setter
public class SalesDistributionSheetParser implements SheetParser {

  private static final int PLANNING_DISTRIBUTION_STARTING_ROW = 3;

  private static List<PlanningDistribution> parsePlanningDistributionRows(
      final MeliSheet sheet,
      final LogisticCenterConfiguration config
  ) {
    return sheet.getRowsStartingFrom(PLANNING_DISTRIBUTION_STARTING_ROW).stream()
        .filter(item -> rowIsNotEmpty(item, sheet))
        .map(row -> createPlanningDistributionFrom(row, sheet, config))
        .collect(toList());
  }

  private static boolean rowIsNotEmpty(final MeliRow row, final MeliSheet sheet) {
    final Range<Integer> columns = Range.between(1, ForecastProcessPathConfiguration.maxValidateColumnIndex(sheet, row));
    return row.getCells().stream()
        .filter(meliCell -> columns.contains(meliCell.getColumnIndex()))
        .map(MeliCell::getValue)
        .filter(Objects::nonNull)
        .anyMatch(value -> !value.isEmpty());
  }

  private static PlanningDistribution createPlanningDistributionFrom(
      final MeliRow row,
      final MeliSheet sheet,
      final LogisticCenterConfiguration config
  ) {
    final ZoneId zoneId = config.getZoneId();

    final ProcessPath processPath = ForecastProcessPathConfiguration.hasProcessPath(sheet, row)
        ? ProcessPath.from(getStringValueAt(sheet, row, PROCESS_PATH_ID_COLUMN.getPosition(sheet, row))) : GLOBAL;
    final String carrierId = getStringValueAt(sheet, row, CARRIER_ID_COLUMN.getPosition(sheet, row));
    final String serviceId = String.valueOf(getIntValueAt(sheet, row, SERVICE_ID_COLUMN.getPosition(sheet, row)));
    final String canalization = getStringValueAt(sheet, row, CANALIZATION_COLUMN.getPosition(sheet, row));
    final double quantity = Precision.round(getDoubleValueAt(sheet, row.getIndex(), PD_QUANTITY_COLUMN.getPosition(sheet, row)), 2);

    return PlanningDistribution.builder()
        .dateOut(SpreadsheetUtils.getDateTimeAt(sheet, row, PD_DATE_OUT_COLUMN.getPosition(sheet, row), zoneId))
        .dateIn(SpreadsheetUtils.getDateTimeAt(sheet, row, PD_DATE_IN_COLUMN.getPosition(sheet, row), zoneId))
        .carrierId(carrierId)
        .serviceId(serviceId)
        .canalization(canalization)
        .processPath(processPath)
        .quantity(quantity)
        .quantityMetricUnit(MetricUnit.UNITS.getName())
        .metadata(List.of())
        .build();
  }

  @Override
  public String name() {
    return "Distribucion ventas";
  }

  @Override
  public ForecastSheetDto parse(
      final String logisticCenterId,
      final MeliSheet sheet,
      final LogisticCenterConfiguration config) {
    return new ForecastSheetDto(
        sheet.getSheetName(),
        Map.of(PLANNING_DISTRIBUTION, parsePlanningDistributionRows(sheet, config))
    );
  }
}
