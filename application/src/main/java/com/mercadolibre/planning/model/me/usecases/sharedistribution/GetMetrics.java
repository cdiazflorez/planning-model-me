package com.mercadolibre.planning.model.me.usecases.sharedistribution;

import com.mercadolibre.planning.model.me.entities.sharedistribution.ShareDistribution;
import com.mercadolibre.planning.model.me.gateways.sharedistribution.ShareDistributionGateway;
import com.mercadolibre.planning.model.me.gateways.sharedistribution.dto.DistributionResponse;
import com.mercadolibre.planning.model.me.utils.DateUtils;
import java.time.temporal.ChronoUnit;
import java.util.stream.LongStream;
import lombok.AllArgsConstructor;

import javax.inject.Named;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Named
@AllArgsConstructor
public class GetMetrics {

    private final ShareDistributionGateway shareDistributionGateway;

    public List<ShareDistribution> execute(String warehouseId,ZonedDateTime dateFrom, ZonedDateTime dateTo) {

        List<DistributionResponse> distributionResponses = shareDistributionGateway.getMetrics(warehouseId);
        List<ZonedDateTime> dateTimeList = getDaysToProject(dateFrom,dateTo);

        Map<String, List<DistributionResponse>> listDistribution = distributionResponses.stream().collect(Collectors.groupingBy(this::getKey));

        List<ShareDistribution> response = new ArrayList<>();
        for(ZonedDateTime z: dateTimeList){
            List<DistributionResponse> distributionResponsesDay = listDistribution.get(z.getDayOfWeek().name());
            if(distributionResponsesDay != null && !distributionResponsesDay.isEmpty()) {
                Map<String, List<DistributionResponse>> mapDistributionResponse = distributionResponsesDay.stream().collect(Collectors.groupingBy(this::getKeyHour));
                response.addAll(mapDistributionResponse.values().stream().flatMap(value -> filterMetrics(value, z))
                    .collect(Collectors.toList()));
            }
        }

        return response;

    }

    private List<ZonedDateTime> getDaysToProject(ZonedDateTime dateFrom, ZonedDateTime dateTo){
        long n = ChronoUnit.DAYS.between(dateFrom,dateTo);
        return LongStream.range(0,n).mapToObj(dateFrom::plusDays).collect(Collectors.toList());
    }


    private String getKeyHour(DistributionResponse distribution) {
        return distribution.getCptTime().getHour() + "_" + distribution.getCptTime().getMinute();
    }

    private String getKey(DistributionResponse distribution) {
        return distribution.getCptTime().getDayOfWeek().name();
    }

    private Stream<ShareDistribution> filterMetrics(List<DistributionResponse> list, ZonedDateTime dateTime) {

        Map<String, Double> shareDistributionByArea =
                list.stream().collect(Collectors.groupingBy(DistributionResponse::getArea, Collectors.summingDouble(DistributionResponse::getSis)));

        Double total = shareDistributionByArea.values().stream().reduce(0D, Double::sum);

        ZonedDateTime cptTime = list.get(0).getCptTime();

        List<ShareDistribution> shareDistributionList = new ArrayList<>();
        for (String area : shareDistributionByArea.keySet()) {
                shareDistributionList.add(ShareDistribution.builder()
                        .logisticCenterId(list.get(0).getWarehouseID())
                        .date(dateTime.withHour(cptTime.getHour()).withMinute(cptTime.getMinute()))
                        .processName("PICKING")
                        .area(area)
                        .quantity(Math.round(shareDistributionByArea.get(area) / total * 100.00) / 100.00)
                        .quantityMetricUnit("PERCENTAGE")
                        .build());
        }
        return shareDistributionList.stream();

    }


}
