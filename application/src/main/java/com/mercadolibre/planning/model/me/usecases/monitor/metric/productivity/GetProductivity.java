package com.mercadolibre.planning.model.me.usecases.monitor.metric.productivity;

import com.mercadolibre.planning.model.me.entities.projection.UnitsResume;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
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

    @Override
    public Metric execute(ProductivityInput input) {
        if (input.getProcessedUnitLastHour() == null) {
            return GetMetric.createEmptyMetric(PRODUCTIVITY, input.getProcessInfo());
        }
        final ZonedDateTime current = ZonedDateTime.now(ZoneOffset.UTC).withSecond(0).withNano(0);
        final ZonedDateTime utcDateTo = current.with(ChronoField.MINUTE_OF_HOUR, 0);

        return calculateMetric(current, List.of(utcDateTo,
                current.minusHours(1).withSecond(0).withNano(0)), input.headcounts,
                input.getProcessedUnitLastHour(), input.getProcessInfo());
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
        return productivities.stream().reduce(0d, Double::sum);
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
                        && hCount.getProcessName().getName().equalsIgnoreCase(unit.getProcess()
                        .getRelatedProcessName())).findFirst().orElse(null);
        if (Objects.nonNull(headcountForHour)) {
            return headcountForHour.getValue();
        }
        return 0;
    }

}
