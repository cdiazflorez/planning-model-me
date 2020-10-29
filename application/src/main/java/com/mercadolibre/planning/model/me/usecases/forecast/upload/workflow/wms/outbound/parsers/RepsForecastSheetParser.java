package com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.parsers;

import com.mercadolibre.planning.model.me.exception.UnmatchedWarehouseException;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.AreaDistribution;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.HeadcountDistribution;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.HeadcountProductivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.HeadcountProductivityData;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PolyvalentProductivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingDistribution;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingDistributionData;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.dto.ForecastSheetDto;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.parsers.SheetParser;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastHeadcountProcessName;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastProcessName;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastProcessType;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastProductivityProcessName;
import com.mercadolibre.spreadsheet.MeliSheet;
import lombok.AllArgsConstructor;

import javax.inject.Named;

import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.utils.SpreadsheetUtils.formatter;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.utils.SpreadsheetUtils.getIntValueAt;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.utils.SpreadsheetUtils.getLongValueAt;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.utils.SpreadsheetUtils.getValueAt;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.utils.SpreadsheetUtils.timeFormatter;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastColumnName.HEADCOUNT_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastColumnName.HEADCOUNT_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastColumnName.MONO_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastColumnName.MULTI_BATCH_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastColumnName.MULTI_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastColumnName.POLYVALENT_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastColumnName.PROCESSING_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastColumnName.WEEK;

@Named
@AllArgsConstructor
public class RepsForecastSheetParser implements SheetParser {

    private static final int DEFAULT_ABILITY_LEVEL = 1;
    private static final int POLYVALENT_ABILITY_LEVEL = 2;
    private static final int PROCESSING_DISTRIBUTION_STARTING_ROW = 7;
    private static final int HOURS_PER_FORECAST_PERIOD = 168;
    private static final int POLYVALENT_PRODUCTIVITY_STARTING_ROW = 181;
    private static final int HEADCOUNT_PRODUCTIVITY_STARTING_ROW = 184;

    private final LogisticCenterGateway logisticCenterGateway;

    @Override
    public String name() {
        return "Reps";
    }

    @Override
    public ForecastSheetDto parse(final String warehouseId, final MeliSheet sheet) {
        validateIfWarehouseIdIsCorrect(warehouseId, sheet);

        final LogisticCenterConfiguration config =
                logisticCenterGateway.getConfiguration(warehouseId);

        return new ForecastSheetDto(
                sheet.getSheetName(),
                Map.of(
                        WEEK, getValueAt(sheet, 2, 2),
                        MONO_ORDER_DISTRIBUTION, getLongValueAt(sheet, 3, 5),
                        MULTI_BATCH_DISTRIBUTION, getLongValueAt(sheet, 3, 6),
                        MULTI_ORDER_DISTRIBUTION, getLongValueAt(sheet, 3, 7),
                        PROCESSING_DISTRIBUTION, getProcessingDistribution(config, sheet),
                        HEADCOUNT_DISTRIBUTION, getHeadcountDistribution(sheet),
                        POLYVALENT_PRODUCTIVITY, getPolyvalentProductivity(sheet),
                        HEADCOUNT_PRODUCTIVITY, getHeadcountProductivity(config, sheet)
                )
        );
    }

    private void validateIfWarehouseIdIsCorrect(String warehouseId, MeliSheet sheet) {
        final String warehouseIdFromSheet = getValueAt(sheet, 3, 2);
        boolean warehouseIdsAreDifferent = !warehouseIdFromSheet.equalsIgnoreCase(warehouseId);
        if (isNullOrEmpty(warehouseIdFromSheet) || warehouseIdsAreDifferent) {
            throw new UnmatchedWarehouseException(warehouseId, warehouseIdFromSheet);
        }
    }

    private List<ProcessingDistribution> getProcessingDistribution(
            final LogisticCenterConfiguration config, final MeliSheet sheet) {

        final List<ProcessingDistribution> processingDistributions = new ArrayList<>();

        ForecastProcessName.stream().forEach(forecastProcessName
                -> forecastProcessName.getProcessTypes().forEach(forecastProcessType
                        -> processingDistributions.add(ProcessingDistribution.builder()
                        .processName(forecastProcessName.toString())
                        .type(forecastProcessType.toString())
                        .quantityMetricUnit(forecastProcessType.getMetricUnit().getName())
                        .data(new ArrayList<>())
                        .build()
        )));

        for (int i = 0; i < HOURS_PER_FORECAST_PERIOD; i++) {
            final int rowIndex = PROCESSING_DISTRIBUTION_STARTING_ROW + i;

            processingDistributions
                    .forEach(processingDistribution -> {
                        final int columnIndex = getColumnIndex(processingDistribution);

                        processingDistribution.getData().add(ProcessingDistributionData.builder()
                                .date(ZonedDateTime
                                        .parse(getValueAt(sheet, rowIndex, 1),
                                                formatter.withZone(config.getZoneId())
                                        )
                                )
                                .quantity(getIntValueAt(sheet, rowIndex, columnIndex))
                                .build()
                        );
                    });
        }

        return processingDistributions;
    }

    private int getColumnIndex(final ProcessingDistribution processingDistribution) {
        return ForecastProcessName.from(processingDistribution.getProcessName()).getStartingColumn()
                + ForecastProcessType.from(processingDistribution.getType()).getColumnOrder();
    }

    private List<HeadcountDistribution> getHeadcountDistribution(final MeliSheet sheet) {
        return Arrays.stream(ForecastHeadcountProcessName.values())
                .map(headcountProcessName -> HeadcountDistribution.builder()
                        .processName(headcountProcessName.getName())
                        .quantityMetricUnit(MetricUnit.PERCENTAGE.getName())
                        .areas(createAreaDistributionFrom(sheet, headcountProcessName))
                        .build()
                )
                .collect(Collectors.toList());
    }

    private List<AreaDistribution> createAreaDistributionFrom(
            final MeliSheet sheet,
            final ForecastHeadcountProcessName headcountProcessName) {
        return headcountProcessName.getAreas().stream()
                .map(area -> AreaDistribution.builder()
                        .areaId(area.getName())
                        .quantity(getLongValueAt(
                                sheet,
                                headcountProcessName.getRowIndex(),
                                area.getColumnIndex())
                        )
                        .build()
                )
                .collect(Collectors.toList());
    }

    private List<PolyvalentProductivity> getPolyvalentProductivity(final MeliSheet sheet) {
        return Arrays.stream(ForecastProductivityProcessName.values())
                .map(productivityProcess -> PolyvalentProductivity.builder()
                        .abilityLevel(POLYVALENT_ABILITY_LEVEL)
                        .processName(productivityProcess.getName())
                        .productivityMetricUnit(MetricUnit.PERCENTAGE.getName())
                        .productivity(getLongValueAt(
                                sheet,
                                POLYVALENT_PRODUCTIVITY_STARTING_ROW,
                                productivityProcess.getColumnIndex())
                        )
                        .build()
                )
                .collect(Collectors.toList());
    }

    private List<HeadcountProductivity> getHeadcountProductivity(
            final LogisticCenterConfiguration config,
            final MeliSheet sheet) {

        return  Arrays.stream(ForecastProductivityProcessName.values())
                .map(headcountProcessName -> HeadcountProductivity.builder()
                        .processName(headcountProcessName.getName())
                        .abilityLevel(DEFAULT_ABILITY_LEVEL)
                        .productivityMetricUnit(MetricUnit.UNITS_PER_HOUR.getName())
                        .data(createHeadcountProductivityFrom(config, sheet, headcountProcessName))
                        .build()
                )
                .collect(Collectors.toList());
    }

    private List<HeadcountProductivityData> createHeadcountProductivityFrom(
            final LogisticCenterConfiguration config,
            final MeliSheet sheet,
            final ForecastProductivityProcessName productivityProcessName) {

        final List<HeadcountProductivityData> headcountProductivityData = new ArrayList<>();
        final ZoneId zoneId = config.getTimeZone().toZoneId();

        for (int hour = 0; hour < 24; hour++) {
            final int row = HEADCOUNT_PRODUCTIVITY_STARTING_ROW + hour;
            headcountProductivityData.add(
                    HeadcountProductivityData.builder()
                            .dayTime(getUtcOffset(OffsetTime.of(
                                    LocalTime.parse(getValueAt(sheet, row, 1), timeFormatter),
                                    zoneId.getRules().getOffset(Instant.now()))))
                            .productivity(getLongValueAt(
                                    sheet,
                                    row,
                                    productivityProcessName.getColumnIndex())
                            )
                            .build()
            );
        }

        return headcountProductivityData;
    }

    public static OffsetTime getUtcOffset(final OffsetTime offsetTime) {
        return offsetTime.withOffsetSameInstant(ZoneOffset.UTC);
    }
}
