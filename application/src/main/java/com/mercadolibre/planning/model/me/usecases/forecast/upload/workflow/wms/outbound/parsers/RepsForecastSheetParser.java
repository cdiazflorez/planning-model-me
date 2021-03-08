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
import com.mercadolibre.planning.model.me.usecases.forecast.upload.dto.RepsDistributionDto;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.parsers.SheetParser;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.utils.SpreadsheetUtils;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastHeadcountProcessName;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastProcessName;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastProcessType;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastProductivityProcessName;
import com.mercadolibre.spreadsheet.MeliSheet;
import lombok.AllArgsConstructor;

import javax.inject.Named;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.utils.SpreadsheetUtils.getDoubleValueAt;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.utils.SpreadsheetUtils.getIntValueAt;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.utils.SpreadsheetUtils.getIntValueAtFromDuration;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.utils.SpreadsheetUtils.getLongValueAt;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.utils.SpreadsheetUtils.getStringValueAt;
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
    private static final int POLYVALENT_PRODUCTIVITY_STARTING_ROW = 188;
    private static final int HEADCOUNT_PRODUCTIVITY_COLUMN_OFFSET = 3;

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

        final RepsDistributionDto repsDistributionDto = getProcessingDistribution(config, sheet);

        return new ForecastSheetDto(
                sheet.getSheetName(),
                Map.of(
                        WEEK, getStringValueAt(sheet, 2, 2),
                        MONO_ORDER_DISTRIBUTION, getDoubleValueAt(sheet, 3, 5),
                        MULTI_BATCH_DISTRIBUTION, getDoubleValueAt(sheet, 3, 6),
                        MULTI_ORDER_DISTRIBUTION, getDoubleValueAt(sheet, 3, 7),
                        PROCESSING_DISTRIBUTION, repsDistributionDto.getProcessingDistributions(),
                        HEADCOUNT_DISTRIBUTION, getHeadcountDistribution(sheet),
                        POLYVALENT_PRODUCTIVITY, getPolyvalentProductivity(sheet),
                        HEADCOUNT_PRODUCTIVITY, repsDistributionDto.getHeadcountProductivities()
                )
        );
    }

    private void validateIfWarehouseIdIsCorrect(String warehouseId, MeliSheet sheet) {
        final String warehouseIdFromSheet = getStringValueAt(sheet, 3, 2);
        boolean warehouseIdsAreDifferent = !warehouseIdFromSheet.equalsIgnoreCase(warehouseId);
        if (isNullOrEmpty(warehouseIdFromSheet) || warehouseIdsAreDifferent) {
            throw new UnmatchedWarehouseException(warehouseId, warehouseIdFromSheet);
        }
    }

    private RepsDistributionDto getProcessingDistribution(final LogisticCenterConfiguration config,
                                                          final MeliSheet sheet) {
        // Columns
        final List<ProcessingDistribution> processingDistributions = new ArrayList<>();
        ForecastProcessName.stream().forEach(forecastProcessName
                -> forecastProcessName.getProcessTypes().forEach(forecastProcessType
                        -> processingDistributions.add(ProcessingDistribution.builder()
                                .processName(forecastProcessName.toString())
                                .type(forecastProcessType.toString())
                                .quantityMetricUnit(forecastProcessType.getMetricUnit().getName())
                                .data(new ArrayList<>())
                                .build()
                ))
        );

        final List<HeadcountProductivity> headcountProductivities = new ArrayList<>();
        ForecastProductivityProcessName.stream().forEach(
                processName -> headcountProductivities.add(HeadcountProductivity.builder()
                        .processName(processName.name())
                        .abilityLevel(DEFAULT_ABILITY_LEVEL)
                        .productivityMetricUnit(MetricUnit.UNITS_PER_HOUR.getName())
                        .data(new ArrayList<>())
                        .build()
                )
        );

        final ZoneId zoneId = config.getZoneId();

        // Parse data rows
        for (int i = 0; i < HOURS_PER_FORECAST_PERIOD; i++) {
            final int rowIndex = PROCESSING_DISTRIBUTION_STARTING_ROW + i;

            processingDistributions
                    .forEach(processingDistribution -> {
                        final int columnIndex = getColumnIndex(processingDistribution);

                        processingDistribution.getData().add(ProcessingDistributionData.builder()
                                .date(SpreadsheetUtils.getDateTimeAt(sheet, rowIndex, 1, zoneId))
                                .quantity(getQuantity(sheet, rowIndex, processingDistribution,
                                        columnIndex))
                                .build()
                        );
                    });

            headcountProductivities.forEach(headcountProductivity -> {
                final int columnIndex = ForecastProcessName.from(
                        headcountProductivity.getProcessName()).getStartingColumn()
                        + HEADCOUNT_PRODUCTIVITY_COLUMN_OFFSET;

                headcountProductivity.getData().add(HeadcountProductivityData.builder()
                        .dayTime(SpreadsheetUtils.getDateTimeAt(sheet, rowIndex, 1, zoneId))
                        .productivity(getLongValueAt(sheet, rowIndex, columnIndex))
                        .build()
                );
            });
        }

        return new RepsDistributionDto(processingDistributions, headcountProductivities);
    }

    private int getQuantity(final MeliSheet sheet, final int rowIndex,
            ProcessingDistribution processingDistribution, final int columnIndex) {

        return isRemainingProcessing(processingDistribution)
                ? getIntValueAtFromDuration(sheet, rowIndex, columnIndex)
                        : getIntValueAt(sheet, rowIndex, columnIndex);
    }

    private boolean isRemainingProcessing(ProcessingDistribution processingDistribution) {
        return ForecastProcessType.REMAINING_PROCESSING
                == ForecastProcessType.from(processingDistribution.getType())
                && MetricUnit.MINUTES
                == MetricUnit.from(processingDistribution.getQuantityMetricUnit());
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
                        .quantity(getDoubleValueAt(
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
                        .productivity(getDoubleValueAt(
                                sheet,
                                POLYVALENT_PRODUCTIVITY_STARTING_ROW,
                                productivityProcess.getColumnIndex())
                        )
                        .build()
                )
                .collect(Collectors.toList());
    }

}
