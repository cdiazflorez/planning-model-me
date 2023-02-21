package com.mercadolibre.planning.model.me.usecases.backlog;

import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.DATE_OUT;
import static java.util.List.of;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.workflows.BacklogWorkflow;
import com.mercadolibre.planning.model.me.entities.workflows.Step;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogLastPhotoRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogByDateDto;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GetBacklogByDateInboundTest {

  private static final String DATE_BACKLOG = "2022-01-04T12:00:00Z";
  private static final int QUANTITY_BACKLOG = 123;

  @InjectMocks
  private GetBacklogByDateInbound getBacklogByDateInbound;
  @Mock
  private BacklogApiGateway backlogApiGateway;

  @Test
  public void testGetBacklogByDateInbound() {

    final Instant now = Instant.now().truncatedTo(ChronoUnit.MINUTES);
    final String warehouseId = "ARTW01";
    final Set<BacklogWorkflow> workflows = Set.of(BacklogWorkflow.INBOUND, BacklogWorkflow.INBOUND_TRANSFER);
    final Set<Step> processNames = Set.of(Step.CHECK_IN, Step.PUT_AWAY);
    final Set<BacklogGrouper> groupers = Set.of(DATE_OUT);
    final Instant slaFrom = now.minus(30, ChronoUnit.DAYS);
    final Instant slaTo = now.plus(1, ChronoUnit.DAYS);

    when(backlogApiGateway.getLastPhoto(new BacklogLastPhotoRequest(
        warehouseId,
        workflows,
        processNames,
        null,
        null,
        slaFrom,
        slaTo,
        groupers,
        now))).thenReturn(responseGetCurrentBacklog(), (Photo) null);

    List<Backlog> response = getBacklogByDateInbound.execute(new GetBacklogByDateDto(
        Workflow.FBM_WMS_INBOUND, warehouseId, slaFrom, slaTo
    ));

    List<Backlog> responseEmpty = getBacklogByDateInbound.execute(new GetBacklogByDateDto(
        Workflow.FBM_WMS_INBOUND, warehouseId, slaFrom, slaTo
    ));

    Assertions.assertEquals(expectedBacklog(), response);
    Assertions.assertEquals(0, responseEmpty.size());

  }

  private List<Backlog> expectedBacklog() {
    return of(new Backlog(ZonedDateTime.parse(DATE_BACKLOG), QUANTITY_BACKLOG));
  }

  private Photo responseGetCurrentBacklog() {
    return new Photo(
        Instant.now(),
        of(new Photo.Group(Map.of(DATE_OUT, DATE_BACKLOG), QUANTITY_BACKLOG, 0))
    );
  }

}
