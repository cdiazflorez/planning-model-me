package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.*;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import lombok.AllArgsConstructor;

import javax.inject.Named;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;


@Named
@AllArgsConstructor
public class GetBacklogScheduled {
    private static final int SCALE_DECIMAL = 2;
    private static final int AMOUNT_TO_SUBSTRACT_MINUTES = 5;
    private static final int AMOUNT_TO_ADD_MINUTES = 5;
    private static final int AMOUNT_TO_ADD_DAYS = 1;
    private final LogisticCenterGateway logisticCenterGateway;
    private final BacklogApiGateway backlogGateway;


    public BacklogScheduled execute(String logisticCenterId, Instant requestDate) {
        final Instant today = ZonedDateTime.ofInstant(
                        requestDate,
                        logisticCenterGateway.getConfiguration(logisticCenterId).getZoneId()
                )
                .truncatedTo(ChronoUnit.DAYS)
                .toInstant();

        int backlogExpected = 0, remainBacklog = 0;
        for (Map.Entry<Instant, Integer>
                entry : getFirstBacklogPhotoTaken(logisticCenterId, today).entrySet())
            if (entry.getKey().isBefore(requestDate)) {
                backlogExpected = backlogExpected + entry.getValue();
            } else {
                remainBacklog = remainBacklog + entry.getValue();
            }
        final int lateBacklog =
                this.getLateBacklog(logisticCenterId, today, requestDate)
                        .stream()
                        .mapToInt(Consolidation::getTotal)
                        .sum();
        return new BacklogScheduled(
                Indicator.builder().units(backlogExpected)
                .build(),
                getReceivedBacklog(backlogExpected, lateBacklog),
                Indicator.builder().units(remainBacklog).build(),
                Indicator.builder().units(lateBacklog).percentage(getDesvioPercentage(lateBacklog, backlogExpected))
                .build());
    }

    private double getDesvioPercentage(int desvio, int backlogExpected) {
        if( backlogExpected != 0) return new BigDecimal(String.valueOf((double) desvio / (double) backlogExpected))
                .setScale(SCALE_DECIMAL, RoundingMode.FLOOR)
                .doubleValue();
        return 0;
    }

    private List<Consolidation> firstPhotoOfDay(String warehouseId, Instant photoDateFrom,
                                                Instant photoDateTo, Instant today) {

        List<Consolidation> photos = backlogGateway.getBacklog(
                new BacklogRequest(warehouseId, photoDateFrom, photoDateTo)
                        .withWorkflows(List.of("inbound"))
                        .withGroupingFields(List.of("date_in"))
                        .withDateInRange(today, today.plus(AMOUNT_TO_ADD_DAYS, ChronoUnit.DAYS))
        );
        return photos.stream().collect(
                Collectors.groupingBy(
                        Consolidation::getDate,
                        TreeMap::new,
                        Collectors.toList()
                )).firstEntry().getValue();
    }

    private List<Consolidation> getLateBacklog(String warehouse, Instant scheduledDateFrom, Instant scheduledDateTo) {
        return backlogGateway.getCurrentBacklog(
                new BacklogCurrentRequest(warehouse)
                        .withDateInRange(scheduledDateFrom, scheduledDateTo)
                        .withWorkflows(List.of("inbound"))
                        .withSteps(List.of("SCHEDULED"))
                        .withGroupingFields(List.of("process"))
        );
    }

    private Indicator getReceivedBacklog(int expectedBacklog, int lateBacklog) {
        return Indicator
                .builder()
                .units(expectedBacklog - lateBacklog)
                .build();
    }

    private Map<Instant, Integer> getFirstBacklogPhotoTaken(String warehouse, Instant since) {
        final Instant photoDateFrom = since.minus(AMOUNT_TO_SUBSTRACT_MINUTES, ChronoUnit.MINUTES);
        final Instant photoDateTo = since.plus(AMOUNT_TO_ADD_MINUTES, ChronoUnit.MINUTES);
        return this.firstPhotoOfDay(warehouse, photoDateFrom, photoDateTo, since)
                .stream().collect(
                        Collectors.toMap(
                                consolidation -> Instant.parse(consolidation.getKeys().get("date_in")),
                                Consolidation::getTotal,
                                (firstPhotoValue, secondPhotoValue) -> firstPhotoValue,
                                TreeMap::new
                        )
                );
    }
}
