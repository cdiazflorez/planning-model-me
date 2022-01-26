package com.mercadolibre.planning.model.me.usecases.sales;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogFilters;
import com.mercadolibre.planning.model.me.gateways.outboundunit.UnitSearchGateway;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.sales.dtos.GetSalesInputDto;
import lombok.AllArgsConstructor;

import javax.inject.Named;

import java.util.List;

@Named
@AllArgsConstructor
public class GetSales implements UseCase<GetSalesInputDto, List<Backlog>> {

    final UnitSearchGateway unitSearchGateway;

    @Override
    public List<Backlog> execute(final GetSalesInputDto input) {
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
