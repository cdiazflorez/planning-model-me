package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.exception.BacklogGatewayNotSupportedException;
import com.mercadolibre.planning.model.me.gateways.backlog.strategy.BacklogGatewayProvider;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogByDateDto;
import lombok.AllArgsConstructor;

import javax.inject.Named;

import java.util.List;

import static java.time.ZoneOffset.UTC;

@Named
@AllArgsConstructor
public class GetBacklogByDate implements UseCase<GetBacklogByDateDto, List<Backlog>> {

    final BacklogGatewayProvider backlogGatewayProvider;

    @Override
    public List<Backlog> execute(GetBacklogByDateDto input) {
        return backlogGatewayProvider
                .getBy(input.getWorkflow())
                .orElseThrow(() -> new BacklogGatewayNotSupportedException(input.getWorkflow()))
                .getBacklog(
                        input.getWarehouseId(),
                        /* Note that the zone is not necessary but the BacklogGateway requires it to
                        no avail. */
                        input.getDateFrom().atZone(UTC),
                        input.getDateTo().atZone(UTC),
                        List.of("pending"),
                        List.of("etd")
                );
    }
}
