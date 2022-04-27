package com.mercadolibre.planning.model.me.usecases.sharedistribution;

import com.mercadolibre.planning.model.me.entities.sharedistribution.ShareDistribution;
import com.mercadolibre.planning.model.me.gateways.sharedistribution.ShareDistributionGateway;
import com.mercadolibre.planning.model.me.gateways.sharedistribution.dto.DistributionElement;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import javax.inject.Named;
import lombok.AllArgsConstructor;

/** Build projected backlog metrics. */
@Named
@AllArgsConstructor
public class GetMetrics {

  private static final double NUMBER_OF_DECIMALS = 2;

  private static final double TEN_RAISED_TO_THE_NUMBER_OF_DECIMALS = Math.pow(10, NUMBER_OF_DECIMALS);

  private static final int DAYS_IN_WEEK = 7;

  private static final int NUMBER_HISTORICAL_SAMPLES = 4;

  private static final int FIRST_SAMPLE_OFFSET_IN_DAYS = NUMBER_HISTORICAL_SAMPLES * DAYS_IN_WEEK;


  private final ShareDistributionGateway shareDistributionGateway;

  public List<ShareDistribution> execute(String warehouseId, Instant dateFrom, Instant dateTo) {

    Instant dateToQuery = dateFrom.minus(24, ChronoUnit.HOURS);
    Instant dateFromQuery = dateToQuery.minus(FIRST_SAMPLE_OFFSET_IN_DAYS, ChronoUnit.DAYS);

    List<DistributionElement> distributionElements = shareDistributionGateway.getMetrics(warehouseId, dateFromQuery, dateToQuery);
    List<Instant> dateTimeList = buildProjectionTimePartition(dateFrom, dateTo);

    Map<DayOfWeek, List<DistributionElement>> distributionsByCptWeekDay =
        distributionElements.stream().collect(Collectors.groupingBy(this::getCptWeekDayOf));

    List<ShareDistribution> response = new ArrayList<>();
    for (Instant z : dateTimeList) {
      LocalDateTime localDateTime = LocalDateTime.ofInstant(z, ZoneOffset.UTC);
      List<DistributionElement> distributionAtCptWeekDay = distributionsByCptWeekDay.get(localDateTime.getDayOfWeek());
      if (distributionAtCptWeekDay != null && !distributionAtCptWeekDay.isEmpty()) {
        Map<String, List<DistributionElement>> distributionAtDayByTime =
            distributionAtCptWeekDay.stream().collect(Collectors.groupingBy(this::getCptTimeOf));
        response.addAll(distributionAtDayByTime.values().stream()
            .flatMap(value -> splitProportionByAreaAndDate(value, ZonedDateTime.ofInstant(z, ZoneOffset.UTC)))
            .collect(Collectors.toList()));
      }
    }

    return response;

  }

  private List<Instant> buildProjectionTimePartition(Instant dateFrom, Instant dateTo) {
    long n = ChronoUnit.DAYS.between(dateFrom, dateTo);
    return LongStream.range(0, n).mapToObj(d -> dateFrom.plus(d, ChronoUnit.DAYS)).collect(Collectors.toList());
  }


  private String getCptTimeOf(DistributionElement distribution) {
    return distribution.getCptTime().getHour() + "_" + distribution.getCptTime().getMinute();
  }

  private DayOfWeek getCptWeekDayOf(DistributionElement distribution) {
    return distribution.getCptTime().getDayOfWeek();
  }

  private Stream<ShareDistribution> splitProportionByAreaAndDate(List<DistributionElement> list, ZonedDateTime dateTime) {

    final Map<String, Double> shareDistributionByArea =
        list.stream().collect(Collectors.groupingBy(DistributionElement::getArea, Collectors.summingDouble(DistributionElement::getSis)));

    final Double total = shareDistributionByArea.values().stream().reduce(0D, Double::sum);

    final ZonedDateTime cptTime = list.get(0).getCptTime();

    return shareDistributionByArea.entrySet().stream().map(value -> ShareDistribution.builder()
        .logisticCenterId(list.get(0).getWarehouseID())
        .date(dateTime.withHour(cptTime.getHour()).withMinute(cptTime.getMinute()))
        .processName("PICKING")
        .area(value.getKey())
        .quantity(Math.round(value.getValue() / total * TEN_RAISED_TO_THE_NUMBER_OF_DECIMALS) / TEN_RAISED_TO_THE_NUMBER_OF_DECIMALS)
        .quantityMetricUnit("PERCENTAGE")
        .build());
  }


}
