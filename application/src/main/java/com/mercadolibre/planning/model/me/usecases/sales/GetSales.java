package com.mercadolibre.planning.model.me.usecases.sales;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.exception.BacklogGatewayNotSupportedException;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogFilters;
import com.mercadolibre.planning.model.me.gateways.backlog.strategy.BacklogGatewayProvider;
import com.mercadolibre.planning.model.me.gateways.outboundunit.UnitSearchGateway;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.sales.dtos.GetSalesInputDto;
import lombok.AllArgsConstructor;

import javax.inject.Named;

import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;

import static java.util.Optional.ofNullable;

@Named
@AllArgsConstructor
public class GetSales implements UseCase<GetSalesInputDto, List<Backlog>> {

    final BacklogGatewayProvider backlogGatewayProvider;

    final UnitSearchGateway unitSearchGateway;

    @Override
    public List<Backlog> execute(final GetSalesInputDto input) {
        final List<Backlog> sales = new LinkedList<>();
        ofNullable(input.getFromDS()).ifPresentOrElse(
                fromDS -> getFromUnitSearch(input, sales),
                () -> getFromOutboundUnit(input, sales)
        );
        return sales;
    }

    private void getFromOutboundUnit(final GetSalesInputDto input,
                                     final List<Backlog> sales) {
        sales.addAll(backlogGatewayProvider
                .getBy(input.getWorkflow())
                .orElseThrow(() -> new BacklogGatewayNotSupportedException(input.getWorkflow()))
                .getSalesByCpt(BacklogFilters.builder()
                        .dateCreatedFrom(input.getDateCreatedFrom())
                        .dateCreatedTo(input.getDateCreatedTo())
                        .cptFrom(input.getDateOutFrom())
                        .cptTo(input.getDateOutTo())
                        .warehouseId(input.getWarehouseId())
                        .groupType("order")
                        .build()
                ));
    }

    private void getFromUnitSearch(final GetSalesInputDto input,
                                   final List<Backlog> sales) {
        sales.addAll(unitSearchGateway
                .getSalesByCpt(BacklogFilters.builder()
                        .dateCreatedFrom(input.getDateCreatedFrom())
                        .dateCreatedTo(input.getDateCreatedTo())
                        .cptFrom(input.getDateOutFrom())
                        .cptTo(input.getDateOutTo())
                        .warehouseId(input.getWarehouseId())
                        .groupType("order")
                        .timeZone(input.getTimeZone())
                        .build()
                ));
    }
}
