package com.mercadolibre.planning.model.me.usecases.backlog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.entities.workflows.BacklogWorkflow;
import com.mercadolibre.planning.model.me.entities.workflows.Step;
import com.mercadolibre.planning.model.me.enums.DeviationType;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogLastPhotoRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.ScheduleAdjustment;
import com.mercadolibre.planning.model.me.gateways.planningmodel.DeviationGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Deviation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetActiveDeviationsTest {

  private static final String WAREHOUSE_ID = "ARTW01";

  private static final Instant VIEW_DATE = Instant.parse("2023-01-15T10:00:00Z");

  private static final Set<Workflow> INBOUND_WORKFLOWS = Set.of(Workflow.INBOUND, Workflow.INBOUND_TRANSFER);

  @InjectMocks
  GetActiveDeviations activeDeviations;

  @Mock
  private DeviationGateway deviationGateway;

  @Mock
  private BacklogApiGateway backlogGateway;

  private static Set<BacklogWorkflow> toBacklogWorkflow() {
    return GetActiveDeviationsTest.INBOUND_WORKFLOWS.stream()
        .map(Workflow::getBacklogWorkflow)
        .flatMap(Collection::stream)
        .collect(Collectors.toUnmodifiableSet());
  }

  @Test
  void testActiveDeviations() {
    // GIVEN

    final BacklogLastPhotoRequest request = new BacklogLastPhotoRequest(
        WAREHOUSE_ID,
        toBacklogWorkflow(),
        Set.of(Step.SCHEDULED),
        VIEW_DATE,
        VIEW_DATE.plus(1, ChronoUnit.DAYS),
        null,
        null,
        Set.of(BacklogGrouper.WORKFLOW, BacklogGrouper.DATE_IN),
        VIEW_DATE
    );

    when(deviationGateway.getActiveDeviations(INBOUND_WORKFLOWS, WAREHOUSE_ID, VIEW_DATE))
        .thenReturn(getActiveDeviations());

    when(backlogGateway.getLastPhoto(request)).thenReturn(getScheduledBacklogByDateIn());

    // WHEN
    final List<ScheduleAdjustment> obtained = activeDeviations.execute(WAREHOUSE_ID, VIEW_DATE);

    // THEN
    final List<ScheduleAdjustment> expected = getExpectedResponse();
    assertDeviationsBacklogEquals(expected, obtained);
  }

  @Test
  void testGetActiveDeviationsShouldReturnZeroIfNoBacklogIsFound() {
    // GIVEN
    final BacklogLastPhotoRequest request = new BacklogLastPhotoRequest(
        WAREHOUSE_ID,
        toBacklogWorkflow(),
        Set.of(Step.SCHEDULED),
        VIEW_DATE,
        VIEW_DATE.plus(1, ChronoUnit.DAYS),
        null,
        null,
        Set.of(BacklogGrouper.WORKFLOW, BacklogGrouper.DATE_IN),
        VIEW_DATE
    );

    when(deviationGateway.getActiveDeviations(INBOUND_WORKFLOWS, WAREHOUSE_ID, VIEW_DATE))
        .thenReturn(getActiveDeviations());

    when(backlogGateway.getLastPhoto(request)).thenReturn(new Photo(VIEW_DATE, Collections.emptyList()));

    // WHEN
    final List<ScheduleAdjustment> obtained = activeDeviations.execute(WAREHOUSE_ID, VIEW_DATE);

    // THEN
    assertEquals(2, obtained.size());
    assertEquals(0, obtained.get(0).getUnits());
    assertEquals(0, obtained.get(1).getUnits());
  }

  private void assertDeviationsBacklogEquals(List<ScheduleAdjustment> expected, List<ScheduleAdjustment> obtained) {
    assertEquals(expected.size(), obtained.size());
    for (int i = 0; i < expected.size(); i++) {
      assertEquals(expected.get(i).getDateFrom(), obtained.get(i).getDateFrom());
      assertEquals(expected.get(i).getValue(), obtained.get(i).getValue());
      assertEquals(expected.get(i).getType(), obtained.get(i).getType());
      assertEquals(expected.get(i).getUnits(), obtained.get(i).getUnits());
      assertEquals(expected.get(i).getWorkflow(), obtained.get(i).getWorkflow());
      assertEquals(expected.get(i).getDateTo(), obtained.get(i).getDateTo());
    }
  }

  private List<Deviation> getActiveDeviations() {
    return List.of(
        new Deviation(
            Workflow.FBM_WMS_INBOUND,
            DeviationType.MINUTES,
            VIEW_DATE,
            VIEW_DATE.plus(1, ChronoUnit.DAYS),
            0.1
        ),
        new Deviation(
            Workflow.INBOUND_TRANSFER,
            DeviationType.MINUTES,
            VIEW_DATE,
            VIEW_DATE.plus(1, ChronoUnit.DAYS),
            0.15
        ));
  }

  private Photo getScheduledBacklogByDateIn() {
    return new Photo(
        VIEW_DATE,
        List.of(
            new Photo.Group(
                Map.of(
                    BacklogGrouper.DATE_IN, VIEW_DATE.toString(),
                    BacklogGrouper.WORKFLOW, BacklogWorkflow.INBOUND.name()
                ),
                20000,
                25000
            ),
            new Photo.Group(Map.of(
                BacklogGrouper.DATE_IN, VIEW_DATE.plus(1, ChronoUnit.HOURS).toString(),
                BacklogGrouper.WORKFLOW, BacklogWorkflow.INBOUND_TRANSFER.name()
            ),
                20000,
                25000),
            new Photo.Group(Map.of(
                BacklogGrouper.DATE_IN, VIEW_DATE.plus(2, ChronoUnit.HOURS).toString(),
                BacklogGrouper.WORKFLOW, BacklogWorkflow.INBOUND_TRANSFER.name()
            ),
                20000,
                25000))
    );
  }

  private List<ScheduleAdjustment> getExpectedResponse() {
    return List.of(
        new ScheduleAdjustment(
            List.of(Workflow.FBM_WMS_INBOUND),
            DeviationType.MINUTES,
            Collections.emptyList(),
            0.1,
            2000,
            VIEW_DATE,
            VIEW_DATE.plus(1, ChronoUnit.DAYS)
        ),
        new ScheduleAdjustment(
            List.of(Workflow.INBOUND_TRANSFER),
            DeviationType.MINUTES,
            Collections.emptyList(),
            0.15,
            6000,
            VIEW_DATE,
            VIEW_DATE.plus(1, ChronoUnit.DAYS)
        )
    );
  }
}
