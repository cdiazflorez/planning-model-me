package com.mercadolibre.planning.model.me.usecases.sales;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.exception.BacklogGatewayNotSupportedException;
import com.mercadolibre.planning.model.me.gateways.backlog.strategy.BacklogGatewayProvider;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.sales.dtos.GetSalesInputDto;
import lombok.AllArgsConstructor;

import javax.inject.Named;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Named
@AllArgsConstructor
public class GetSales implements UseCase<GetSalesInputDto, List<Backlog>> {

    final BacklogGatewayProvider backlogGatewayProvider;

    @Override
    public List<Backlog> execute(final GetSalesInputDto input) {
        return backlogGatewayProvider
                .getBy(input.getWorkflow())
                .orElseThrow(() -> new BacklogGatewayNotSupportedException(input.getWorkflow()))
                .getSalesByCpt(
                        input.getWarehouseId(),
                        input.getDateFrom().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                );
    }
}
