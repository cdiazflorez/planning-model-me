package com.mercadolibre.planning.model.me.usecases.monitor.metric.productivity;

import com.mercadolibre.planning.model.me.entities.projection.UnitsResume;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.GetMonitorInput;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Metric;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.GetMetric;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.PRODUCTIVITY;

@Slf4j
@Named
@AllArgsConstructor
public class GetProductivity implements GetMetric<ProductivityInput, Metric> {

    private final PlanningModelGateway planningModelGateway;
    private static final List<ProcessingType> PROJECTION_PROCESSING_TYPES =
            List.of(ProcessingType.ACTIVE_WORKERS);
    protected static final List<ProcessName> PROJECTION_PROCESS_NAMES =
            List.of(ProcessName.PICKING, ProcessName.PACKING);

    @Override
    public Metric execute(ProductivityInput input) {
        if (input.getProcessedUnitLastHour() == null) {
            return GetMetric.createEmptyMetric(PRODUCTIVITY, input.getProcessInfo());
        }
        final ZonedDateTime current = ZonedDateTime.now(ZoneOffset.UTC).withSecond(0).withNano(0);
        final ZonedDateTime utcDateTo = current.with(ChronoField.MINUTE_OF_HOUR, 0);
        final ZonedDateTime utcDateFrom = utcDateTo.minusHours(1);

        final List<Entity> headcount = planningModelGateway.getEntities(
                createRequest(input.getMonitorInput(), utcDateFrom, utcDateTo
                ));

        return calculateMetric(current, List.of(utcDateTo,
                current.minusHours(1).withSecond(0).withNano(0)), headcount,
                input.getProcessedUnitLastHour(), input.getProcessInfo());
    }

    private EntityRequest createRequest(final GetMonitorInput input,
                                        final ZonedDateTime dateFrom,
                                        final ZonedDateTime dateTo) {
        return EntityRequest.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .entityType(EntityType.HEADCOUNT)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .processName(PROJECTION_PROCESS_NAMES)
                .processingType(PROJECTION_PROCESSING_TYPES)
                .build();
    }

    private Metric calculateMetric(final ZonedDateTime current,
                                   final List<ZonedDateTime> dates,
                                   final List<Entity> headcount,
                                   final UnitsResume unit,
                                   ProcessInfo processInfo) {
        final List<Double> productivities = getProductivities(current, dates, headcount, unit);

        final double productivity = calculateProductivity(productivities);

        return GetMetric.createMetric(processInfo,
                String.format("%.1f %s", productivity, "uds./h"),
                PRODUCTIVITY);
    }

    private List<Double> getProductivities(final ZonedDateTime current,
                                           final List<ZonedDateTime> dates,
                                           final List<Entity> headcount,
                                           final UnitsResume unit) {
        ZonedDateTime toDate = current;
        final List<Double> productivities = new LinkedList<>();
        for (ZonedDateTime utcDate : dates) {
            long timeFromCurrentHour = utcDate.until(toDate, ChronoUnit.MINUTES);
            productivities.add(calculateProductivityForHour(utcDate.withMinute(0), headcount, unit,
                    timeFromCurrentHour));
            toDate = utcDate;
        }
        return productivities;
    }

    private double calculateProductivity(final List<Double> productivities) {
        double productivitySum = productivities.stream().reduce(0d, Double::sum);
        double productivityCount = productivities.stream().filter(prod -> prod > 0).count();
        return productivitySum != 0 && productivityCount != 0
                ? productivitySum / productivityCount
                : 0;
    }

    private double calculateProductivityForHour(final ZonedDateTime utcDateTo,
                                                final List<Entity> headcount,
                                                final UnitsResume unit,
                                                final long timeFromCurrentHour) {
        final int unitsLastHour = unit.getUnitCount();
        final double percentageCurrentHour = (100.00 * timeFromCurrentHour) / 60;
        final double quantityFromPercentageCurrentHour =
                (unitsLastHour / 100.00) * percentageCurrentHour;
        final int headCountCurrentHour = getProcessHeadcount(utcDateTo, headcount, unit);
        return headCountCurrentHour == 0
                ? 0 : quantityFromPercentageCurrentHour / headCountCurrentHour;
    }

    private int getProcessHeadcount(final ZonedDateTime utcDateFrom, final List<Entity> headcount,
                                    final UnitsResume unit) {
        final Entity headcountForHour = headcount.stream()
                .filter(hCount -> Objects.equals(utcDateFrom, hCount.getDate())
                        && Objects.equals(unit.getProcess().getRelatedProcess().toLowerCase(),
                        hCount.getProcessName().getName())).findFirst().orElse(null);
        if (Objects.nonNull(headcountForHour)) {
            return headcountForHour.getValue();
        }
        return 0;
    }

}
