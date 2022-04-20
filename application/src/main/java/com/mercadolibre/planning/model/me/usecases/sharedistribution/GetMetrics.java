package com.mercadolibre.planning.model.me.usecases.sharedistribution;

import com.mercadolibre.planning.model.me.entities.sharedistribution.ShareDistribution;
import com.mercadolibre.planning.model.me.gateways.sharedistribution.ShareDistributionGateway;
import com.mercadolibre.planning.model.me.gateways.sharedistribution.dto.DistributionResponse;
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

    public List<ShareDistribution> execute(String warehouseId, ZonedDateTime dateFrom, ZonedDateTime dateTo) {

        List<DistributionResponse> distributionResponses = shareDistributionGateway.getMetrics(warehouseId);

        Map<String, List<DistributionResponse>> listDistribution = distributionResponses.stream().collect(Collectors.groupingBy(this::getKey));

        return listDistribution.values().stream().flatMap(this::filterMetrics)
                .collect(Collectors.filtering(shareDistribution ->
                        shareDistribution.getDate().isAfter(dateFrom) &&  shareDistribution.getDate().isBefore(dateTo), Collectors.toList()));

    }

    private String getKey(DistributionResponse distribution) {
        return distribution.getCptTime().getDayOfWeek() + "_" + distribution.getCptTime().getHour() + "_" + distribution.getCptTime().getMinute();
    }

    private Stream<ShareDistribution> filterMetrics(List<DistributionResponse> list) {

        Map<String, Double> shareDistributionByArea =
                list.stream().collect(Collectors.groupingBy(DistributionResponse::getArea, Collectors.summingDouble(DistributionResponse::getSis)));

        Double total = shareDistributionByArea.values().stream().reduce(0D, Double::sum);

        Optional<DistributionResponse> optionalShareDistribution = list.stream().max(Comparator.comparing(DistributionResponse::getCptTime));


        if (optionalShareDistribution.isPresent()) {
            DistributionResponse shareDistribution = optionalShareDistribution.get();
            ZonedDateTime cptTime = shareDistribution.getCptTime().plusWeeks(1);

            List<ShareDistribution> shareDistributionList = new ArrayList<>();
            for (String area : shareDistributionByArea.keySet()) {
                shareDistributionList.add(ShareDistribution.builder()
                        .logisticCenterId(shareDistribution.getWarehouseID())
                        .date(cptTime)
                        .processName("PICKING")
                        .area(area)
                        .quantity(Math.round(shareDistributionByArea.get(area) / total * 100.00) / 100.00)
                        .quantityMetricUnit("PERCENTAGE")
                        .build());
            }
            return shareDistributionList.stream();
        }

        return Stream.empty();

    }


}
