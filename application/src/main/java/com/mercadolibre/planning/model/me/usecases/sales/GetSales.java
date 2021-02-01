package com.mercadolibre.planning.model.me.usecases.sales;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.exception.BacklogGatewayNotSupportedException;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogFilters;
import com.mercadolibre.planning.model.me.gateways.backlog.strategy.BacklogGatewayProvider;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.sales.dtos.GetSalesInputDto;
import lombok.AllArgsConstructor;

import javax.inject.Named;

import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.util.Optional.ofNullable;

@Named
@AllArgsConstructor
public class GetSales implements UseCase<GetSalesInputDto, List<Backlog>> {

    final BacklogGatewayProvider backlogGatewayProvider;

    @Override
    public List<Backlog> execute(final GetSalesInputDto input) {
        return backlogGatewayProvider
                .getBy(input.getWorkflow())
                .orElseThrow(() -> new BacklogGatewayNotSupportedException(input.getWorkflow()))
                .getSalesByCpt(BacklogFilters.builder()
                        .dateCreatedFrom(ofNullable(input.getDateCreatedFrom()).map(date ->
                                        date.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                                .orElse(null))
                        .dateCreatedTo(ofNullable(input.getDateCreatedTo()).map(date ->
                                        date.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                                .orElse(null))
                        .cptFrom(input.getDateOutFrom())
                        .cptTo(input.getDateOutTo())
                        .warehouseId(input.getWarehouseId())
                        .groupType("order")
                        .build()
                );
    }
}
