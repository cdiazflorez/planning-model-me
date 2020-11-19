package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.exception.BacklogGatewayNotSupportedException;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.strategy.BacklogGatewayProvider;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogInputDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetBacklogTest {

    @InjectMocks
    private GetBacklog getBacklog;

    @Mock
    private BacklogGatewayProvider backlogGatewayProvider;

    @Mock
    private BacklogGateway backlogGateway;

    @Test
    public void testExecuteOk() {
        // GIVEN
        final GetBacklogInputDto input = new GetBacklogInputDto(FBM_WMS_OUTBOUND, WAREHOUSE_ID);

        when(backlogGatewayProvider.getBy(input.getWorkflow()))
                .thenReturn(Optional.of(backlogGateway));
        when(backlogGateway.getBacklog(input.getWarehouseId()))
                .thenReturn(List.of(
                        Backlog.builder()
                                .date(ZonedDateTime.parse("2020-10-08T13:00Z[UTC]"))
                                .quantity(2232)
                                .build(),
                        Backlog.builder()
                                .date(ZonedDateTime.parse("2020-10-08T06:00Z[UTC]"))
                                .quantity(1442)
                                .build(),
                        Backlog.builder()
                                .date(ZonedDateTime.parse("2020-10-08T09:00Z[UTC]"))
                                .quantity(725)
                                .build()
                ));

        // WHEN
        final List<Backlog> backlogs = getBacklog.execute(input);

        // THEN
        assertEquals(3, backlogs.size());

        final Backlog backlogCpt1 = backlogs.get(0);
        assertEquals(ZonedDateTime.parse("2020-10-08T13:00Z[UTC]"), backlogCpt1.getDate());
        assertEquals(2232, backlogCpt1.getQuantity());

        final Backlog backlogCpt2 = backlogs.get(1);
        assertEquals(ZonedDateTime.parse("2020-10-08T06:00Z[UTC]"), backlogCpt2.getDate());
        assertEquals(1442, backlogCpt2.getQuantity());

        final Backlog backlogCpt3 = backlogs.get(2);
        assertEquals(ZonedDateTime.parse("2020-10-08T09:00Z[UTC]"), backlogCpt3.getDate());
        assertEquals(725, backlogCpt3.getQuantity());
    }

    @Test
    public void testExecuteWorkflowNotSupported() {
        // GIVEN
        final GetBacklogInputDto input = new GetBacklogInputDto(FBM_WMS_OUTBOUND, WAREHOUSE_ID);

        when(backlogGatewayProvider.getBy(input.getWorkflow()))
                .thenReturn(Optional.empty());

        // WHEN - THEN
        assertThrows(BacklogGatewayNotSupportedException.class, () -> getBacklog.execute(input));
    }
}
