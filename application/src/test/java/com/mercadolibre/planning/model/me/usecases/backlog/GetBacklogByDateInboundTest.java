package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.services.backlog.BacklogApiAdapter;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogByDateDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.DATE_OUT;
import static java.util.List.of;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetBacklogByDateInboundTest {

    private static final String DATE_BACKLOG = "2022-01-04T12:00:00Z";
    private static final int QUANTITY_BACKLOG = 123;
  
    @InjectMocks
    private GetBacklogByDateInbound getBacklogByDateInbound;
    @Mock
    private BacklogApiAdapter backlogApiAdapter;

    @Test
    public void testGetBacklogByDateInbound() {

        final Instant now = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        final String warehouseId = "ARTW01";
        final List<Workflow> workflows = List.of(Workflow.FBM_WMS_INBOUND);
        final List<ProcessName> processNames = List.of(ProcessName.CHECK_IN, ProcessName.PUT_AWAY);
        final Instant dateFrom = now.minus(1, ChronoUnit.HOURS);
        final Instant dateTo = now;
        final Instant slaFrom = now.minus(7, ChronoUnit.DAYS);
        final Instant slaTo = now.plus(1, ChronoUnit.DAYS);

        when(backlogApiAdapter.getCurrentBacklog(
                now,
                warehouseId,
                workflows,
                processNames,
                of(DATE_OUT),
                dateFrom,
                dateTo,
                slaFrom,
                slaTo))
                .thenReturn(responseGetCurrentBacklog());

        List<Backlog> response = getBacklogByDateInbound.execute(new GetBacklogByDateDto(
                Workflow.FBM_WMS_INBOUND, warehouseId, dateFrom, dateTo
        ));

        Assertions.assertEquals(expectedBacklog(), response);
    }

    private List<Backlog> expectedBacklog() {
        return List.of(new Backlog(ZonedDateTime.parse(DATE_BACKLOG), QUANTITY_BACKLOG));
    }

    private List<Consolidation> responseGetCurrentBacklog() {
        return List.of(new Consolidation(Instant.now(), Map.of("date_out", DATE_BACKLOG), QUANTITY_BACKLOG));
    }

}
