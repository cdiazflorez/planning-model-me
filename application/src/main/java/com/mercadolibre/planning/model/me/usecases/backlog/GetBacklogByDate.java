package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.exception.BacklogGatewayNotSupportedException;
import com.mercadolibre.planning.model.me.gateways.backlog.strategy.BacklogGatewayProvider;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogByDateDto;
import lombok.AllArgsConstructor;

import javax.inject.Named;

import java.util.List;

@Named
@AllArgsConstructor
public class GetBacklogByDate implements UseCase<GetBacklogByDateDto, List<Backlog>> {

    final BacklogGatewayProvider backlogGatewayProvider;

    @Override
    public List<Backlog> execute(GetBacklogByDateDto input) {
        return backlogGatewayProvider
                .getBy(input.getWorkflow())
                .orElseThrow(() -> new BacklogGatewayNotSupportedException(input.getWorkflow()))
                .getBacklog(input.getWarehouseId(), input.getDateFrom(), input.getDateTo(), List.of("pending"));
    }
}
