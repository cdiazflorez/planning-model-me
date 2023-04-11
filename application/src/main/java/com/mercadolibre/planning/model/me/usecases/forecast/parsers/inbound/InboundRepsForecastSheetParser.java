package com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound;

import static com.mercadolibre.planning.model.me.enums.ProcessPath.GLOBAL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.HEADCOUNT_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.INBOUND_CHECKIN_PRODUCTIVITY_POLYVALENCES;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.INBOUND_PUTAWAY_PRODUCTIVITY_POLIVALENCES;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.INBOUND_RECEIVING_PRODUCTIVITY_POLYVALENCES;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.POLYVALENT_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.PROCESSING_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.WEEK;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastPolyvalenceInboundProcessName.CHECKIN;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastPolyvalenceInboundProcessName.PUTAWAY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastPolyvalenceInboundProcessName.RECEIVING;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.VERSION;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getDoubleValueAt;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getStringValueAt;

import com.mercadolibre.planning.model.me.exception.ForecastParsingException;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.HeadcountProductivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.HeadcountProductivityData;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PolyvalentProductivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingDistribution;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingDistributionData;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastSheetDto;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.SheetParser;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastPolyvalenceInboundProcessName;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.MetadataCell;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ProcessingDistributionColumn;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.Productivity;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.RepsRow;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.RowGetter;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.RowGetterVersion;
import com.mercadolibre.planning.model.me.usecases.forecast.utils.SheetVersion;
import com.mercadolibre.spreadsheet.MeliSheet;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InboundRepsForecastSheetParser implements SheetParser {

  private static final String TARGET_SHEET = "Plan de Staffing";

  private static final String DELIMITER = ".\n";

  private static final int FIRST_ROW = 7;

  private static final int LAST_ROW = 174;

  private static final int DEFAULT_ABILITY_LEVEL = 1;

  private static final int POLYVALENT_ABILITY_LEVEL = 2;

  private static final int POLYVALENT_PRODUCTIVITY_STARTING_ROW = 180;

  @Override
  public String name() {
    return TARGET_SHEET;
  }

  @Override
  public ForecastSheetDto parse(
      final String requestWarehouseId,
      final MeliSheet sheet,
      final LogisticCenterConfiguration config) {
    final ZoneId zoneId = config.getZoneId();

    final String week = getStringValueAt(
        sheet, MetadataCell.WEEK.getRow(), MetadataCell.WEEK.getColumn());

    final String warehouseId = getStringValueAt(
        sheet, MetadataCell.WAREHOUSE_ID.getRow(), MetadataCell.WAREHOUSE_ID.getColumn());

    final SheetVersion sheetVersion = SheetVersion.getSheetVersion(sheet, FBM_WMS_INBOUND);

    final RowGetterVersion rowGetterVersion = RowGetterVersion.getRowGetterVersion(sheetVersion);

    final RowGetter rowGetter = rowGetterVersion.getRowGetter();

    final List<RepsRow> rows = getRows(sheet, zoneId, sheetVersion, rowGetter);

    checkForErrors(requestWarehouseId, warehouseId, week, rowGetter.getMissingRowValues(rows));

    final Map<String, Double> productivityPolyvalences = getProductivityPolyvalences(sheet).stream()
        .collect(
            Collectors.toMap(
                PolyvalentProductivity::getProcessName,
                PolyvalentProductivity::getProductivity)
        );

    return new ForecastSheetDto(sheet.getSheetName(), Map.of(
        WEEK, week,
        VERSION, sheetVersion.getVersion(),
        PROCESSING_DISTRIBUTION, buildProcessingDistributions(rows, sheetVersion),
        HEADCOUNT_PRODUCTIVITY, buildHeadcountProductivities(rows, rowGetterVersion.getProductivitiesToCalculate()),
        POLYVALENT_PRODUCTIVITY, Collections.emptyList(),
        INBOUND_CHECKIN_PRODUCTIVITY_POLYVALENCES, productivityPolyvalences.get(CHECKIN.getName()),
        INBOUND_PUTAWAY_PRODUCTIVITY_POLIVALENCES, productivityPolyvalences.get(PUTAWAY.getName()),
        INBOUND_RECEIVING_PRODUCTIVITY_POLYVALENCES, productivityPolyvalences.get(RECEIVING.getName())
    ));
  }

  private void checkForErrors(final String requestWarehouseId,
                              final String warehouseId,
                              final String week,
                              final Optional<String> missingValues) {

    final List<String> errorMessages = Stream.of(
        getInvalidWarehouseId(requestWarehouseId, warehouseId),
        getInvalidWeek(week),
        missingValues
    )
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());


    if (!errorMessages.isEmpty()) {
      final String message = String.join(DELIMITER, errorMessages);
      throw new ForecastParsingException(message);
    }
  }

  private Optional<String> getInvalidWeek(final String week) {
    return week.matches(WEEK_FORMAT_REGEX)
        ? Optional.empty()
        : Optional.of("week value is malformed or missing");
  }

  private Optional<String> getInvalidWarehouseId(final String expected, final String actual) {
    return expected.equals(actual)
        ? Optional.empty()
        : Optional.of(String.format(
        "Warehouse id %s is different from warehouse id %s from file",
        expected,
        actual
        )
    );
  }

  private List<RepsRow> getRows(final MeliSheet sheet,
                                final ZoneId zoneId,
                                final SheetVersion sheetVersion,
                                final RowGetter rowGetter) {
    return IntStream.rangeClosed(FIRST_ROW, LAST_ROW)
            .mapToObj(row -> rowGetter.readRows(sheet, zoneId, row, sheetVersion))
            .collect(Collectors.toList());
  }

  private List<HeadcountProductivityData> toHeadcountProductivityData(final List<RepsRow> rows,
                                                                      final Productivity column) {

    return rows.stream()
        .map(row -> new HeadcountProductivityData(
            row.getDate().getValue(),
            column.getMapper().apply(row))
        )
        .collect(Collectors.toList());

  }

  private List<HeadcountProductivity> buildHeadcountProductivities(final List<RepsRow> rows,
                                                                   final List<Productivity> productivities) {
    return productivities.stream()
        .map(column -> new HeadcountProductivity(
                GLOBAL,
                column.getProcess().getName(),
                UNITS_PER_HOUR.getName(),
                DEFAULT_ABILITY_LEVEL,
                toHeadcountProductivityData(rows, column)
            )
        )
        .collect(Collectors.toList());
  }

  private List<ProcessingDistributionData> toProcessingDistributionData(
      final List<RepsRow> rows,
      final ProcessingDistributionColumn column) {

    return rows.stream()
        .map(row -> new ProcessingDistributionData(
                row.getDate().getValue(),
                column.getMapper().apply(row)
            )
        )
        .collect(Collectors.toList());
  }

  private List<ProcessingDistribution> buildProcessingDistributions(final List<RepsRow> rows, final SheetVersion sheetVersion) {

    return Arrays.stream(ProcessingDistributionColumn.values())
        .filter(column -> column.getColumnIdByVersion().get(sheetVersion) > 0)
        .map(column -> new ProcessingDistribution(
                column.getType().name(),
                column.getUnit().getName(),
                column.getProcess().getName(),
                GLOBAL,
                toProcessingDistributionData(rows, column)
            )
        )
        .collect(Collectors.toList());
  }

  private List<PolyvalentProductivity> getProductivityPolyvalences(final MeliSheet sheet) {
    return ForecastPolyvalenceInboundProcessName.stream()
        .map(
            productivityProcess ->
                PolyvalentProductivity.builder()
                    .abilityLevel(POLYVALENT_ABILITY_LEVEL)
                    .processName(productivityProcess.getName())
                    .productivityMetricUnit(MetricUnit.PERCENTAGE.getName())
                    .productivity(
                        getDoubleValueAt(
                            sheet,
                            POLYVALENT_PRODUCTIVITY_STARTING_ROW,
                            productivityProcess.getColumnIndex()))
                    .build())
        .collect(Collectors.toList());
  }
}

