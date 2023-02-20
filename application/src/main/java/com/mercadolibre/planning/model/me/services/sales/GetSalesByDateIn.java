package com.mercadolibre.planning.model.me.services.sales;

import static java.util.Collections.emptyList;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogCurrentRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogFilters;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.outboundunit.UnitSearchGateway;
import com.mercadolibre.planning.model.me.gateways.toogle.FeatureSwitches;
import com.mercadolibre.planning.model.me.services.sales.dtos.GetSalesInputDto;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Named;
import lombok.AllArgsConstructor;

@Named
@AllArgsConstructor
public class GetSalesByDateIn implements UseCase<GetSalesInputDto, List<Backlog>> {

    private static final String DATE_IN = "date_in";

    final UnitSearchGateway unitSearchGateway;

    private final FeatureSwitches featureSwitches;

    private final BacklogApiGateway backlogGateway;

    @Override
    public List<Backlog> execute(final GetSalesInputDto input) {

        if (featureSwitches.shouldCallBacklogApi()) {

            final List<Consolidation> backlogConsolidation = backlogGateway.getCurrentBacklog(new BacklogCurrentRequest(
                    null,
                    input.getWarehouseId(),
                    List.of("outbound-orders"),
                    emptyList(),
                    emptyList(),
                    List.of(DATE_IN),
                    input.getDateCreatedFrom().toInstant(),
                    input.getDateCreatedTo().toInstant(),
                    input.getDateOutFrom().toInstant(),
                    input.getDateOutTo().toInstant()
            ));

            return backlogConsolidation.stream()
                    .map(item -> new Backlog(
                            ZonedDateTime.ofInstant(Instant.parse(item.getKeys().get(DATE_IN)), ZoneOffset.UTC),
                            item.getTotal()))
                    .collect(Collectors.toList());
        } else {

            return unitSearchGateway.getSalesByCpt(BacklogFilters.builder()
                    .dateCreatedFrom(input.getDateCreatedFrom())
                    .dateCreatedTo(input.getDateCreatedTo())
                    .cptFrom(input.getDateOutFrom())
                    .cptTo(input.getDateOutTo())
                    .warehouseId(input.getWarehouseId())
                    .groupType("order")
                    .timeZone(input.getTimeZone())
                    .build());
        }
    }
}
