package com.mercadolibre.planning.model.me.usecases.backlog;

import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.ZoneOffset.UTC;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.entities.workflows.Step;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogLastPhotoRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Indicator;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo;
import com.mercadolibre.planning.model.me.gateways.inboundreports.InboundReportsApiGateway;
import com.mercadolibre.planning.model.me.gateways.inboundreports.dto.InboundResponse;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper;
import com.mercadolibre.planning.model.me.usecases.backlog.entities.BacklogScheduledMetrics;
import com.mercadolibre.planning.model.me.usecases.backlog.entities.InboundBacklogMonitor;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GetBacklogScheduledTest {

  private static final int QUANTITY_BACKLOG_CURRENT = 200;
  private static final String IB = "inbound";
  private static final String IB_TRANSFER = "INBOUND-TRANSFER";
  private static final String TOTAL_SHIPPED = "total_shipped_quantity";
  private static final Instant INITIAL = Instant.parse("2022-12-27T00:00:00Z");
  private static final Instant NOW = Instant.parse("2022-12-27T08:00:00Z");
  private static final Instant TODAY = Instant.parse("2022-12-27T00:00:00Z");
  private static final Instant TOMORROW = Instant.parse("2022-12-28T00:00:00Z");

  @InjectMocks
  private GetBacklogScheduled getBacklogScheduled;

  @Mock
  private BacklogApiGateway backlogGateway;

  @Mock
  private LogisticCenterGateway logisticCenterGateway;

  @Mock
  private InboundReportsApiGateway inboundReportsApiGateway;

  @Test
  public void backlogScheduledTest() {
    //WHEN
    when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
        .thenReturn(new LogisticCenterConfiguration(TimeZone.getTimeZone(UTC)));

    when(backlogGateway.getLastPhoto(any(BacklogLastPhotoRequest.class))).thenReturn(responseGetCurrentBacklog(true));

    when(inboundReportsApiGateway.getUnitsReceived(WAREHOUSE_ID, INITIAL, NOW, null))
        .thenReturn(new InboundResponse(List.of(new InboundResponse.Aggregation(TOTAL_SHIPPED, 1600))));
    when(inboundReportsApiGateway.getUnitsReceived(WAREHOUSE_ID, INITIAL, NOW, "transfer"))
        .thenReturn(new InboundResponse(List.of(new InboundResponse.Aggregation(TOTAL_SHIPPED, 800))));

    final InboundBacklogMonitor response = getBacklogScheduled.execute(WAREHOUSE_ID, NOW);

    //expected
    BacklogScheduledMetrics expectedBacklogInbound = BacklogScheduledMetrics.builder()
        .expected(Indicator.builder().units(1200).build())
        .received(Indicator.builder().units(800).build())
        .deviation(Indicator.builder().units(400).percentage(-0.33).build())
        .build();

    BacklogScheduledMetrics expectedBacklogInboundTransfer = BacklogScheduledMetrics.builder()
        .expected(Indicator.builder().units(1200).build())
        .received(Indicator.builder().units(800).build())
        .deviation(Indicator.builder().units(400).percentage(-0.33).build())
        .build();

    BacklogScheduledMetrics expectedTotalBacklog = BacklogScheduledMetrics.builder()
        .expected(Indicator.builder().units(2400).build())
        .received(Indicator.builder().units(1600).build())
        .deviation(Indicator.builder().units(800).percentage(-0.33).build())
        .build();

    //verify
    Assertions.assertEquals(3, response.getScheduled().size());
    Assertions.assertEquals(expectedBacklogInbound, response.getScheduled().get(0).getInbound());
    Assertions.assertEquals(expectedBacklogInboundTransfer, response.getScheduled().get(0).getInboundTransfer());
    Assertions.assertEquals(expectedTotalBacklog, response.getScheduled().get(0).getTotal());
  }

  @Test
  public void testBacklogScheduledWithoutTransferBacklog() {
    //WHEN
    when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
        .thenReturn(new LogisticCenterConfiguration(TimeZone.getTimeZone(UTC)));

    when(backlogGateway.getLastPhoto(any(BacklogLastPhotoRequest.class))).thenReturn(responseGetCurrentBacklog(false));
    when(inboundReportsApiGateway.getUnitsReceived(WAREHOUSE_ID, INITIAL, NOW, null))
        .thenReturn(new InboundResponse(List.of(new InboundResponse.Aggregation(TOTAL_SHIPPED, 800))));
    when(inboundReportsApiGateway.getUnitsReceived(WAREHOUSE_ID, INITIAL, NOW, "transfer"))
        .thenReturn(new InboundResponse(List.of()));

    final InboundBacklogMonitor response = getBacklogScheduled.execute(WAREHOUSE_ID, NOW);

    //expected
    BacklogScheduledMetrics expectedBacklogInbound = BacklogScheduledMetrics.builder()
        .expected(Indicator.builder().units(1200).build())
        .received(Indicator.builder().units(800).build())
        .deviation(Indicator.builder().units(400).percentage(-0.33).build())
        .build();

    BacklogScheduledMetrics expectedBacklogInboundTransfer = BacklogScheduledMetrics.builder()
        .expected(Indicator.builder().units(0).build())
        .received(Indicator.builder().units(0).build())
        .deviation(Indicator.builder().units(0).percentage(null).build())
        .build();

    //verify
    Assertions.assertEquals(3, response.getScheduled().size());
    Assertions.assertEquals(expectedBacklogInbound, response.getScheduled().get(0).getInbound());
    Assertions.assertEquals(expectedBacklogInboundTransfer, response.getScheduled().get(0).getInboundTransfer());
    Assertions.assertEquals(expectedBacklogInbound, response.getScheduled().get(0).getTotal());
  }

  private Photo responseGetCurrentBacklog(boolean transfer) {

    final Photo photo = new Photo(NOW, Stream.of(
        new Photo.Group(
            Map.of(
                BacklogGrouper.DATE_IN, TODAY.toString(),
                BacklogGrouper.DATE_OUT, TODAY.plus(1, ChronoUnit.DAYS).toString(),
                BacklogGrouper.WORKFLOW, IB,
                BacklogGrouper.STEP, Step.SCHEDULED.getName()),
            QUANTITY_BACKLOG_CURRENT,
            0
        ),
        new Photo.Group(
            Map.of(
                BacklogGrouper.DATE_IN, TODAY.toString(),
                BacklogGrouper.DATE_OUT, TODAY.plus(1, ChronoUnit.DAYS).toString(),
                BacklogGrouper.WORKFLOW, IB,
                BacklogGrouper.STEP, Step.CHECK_IN.getName()),
            QUANTITY_BACKLOG_CURRENT,
            0
        ),
        new Photo.Group(
            Map.of(
                BacklogGrouper.DATE_IN, TODAY.toString(),
                BacklogGrouper.DATE_OUT, TODAY.plus(1, ChronoUnit.DAYS).toString(),
                BacklogGrouper.WORKFLOW, IB,
                BacklogGrouper.STEP, Step.PUT_AWAY.getName()),
            QUANTITY_BACKLOG_CURRENT,
            0
        ),
        new Photo.Group(
            Map.of(
                BacklogGrouper.DATE_IN, NOW.toString(),
                BacklogGrouper.DATE_OUT, NOW.plus(1, ChronoUnit.DAYS).toString(),
                BacklogGrouper.WORKFLOW, IB,
                BacklogGrouper.STEP, Step.SCHEDULED.getName()),
            QUANTITY_BACKLOG_CURRENT,
            0
        ),
        new Photo.Group(
            Map.of(
                BacklogGrouper.DATE_IN, NOW.toString(),
                BacklogGrouper.DATE_OUT, NOW.plus(1, ChronoUnit.DAYS).toString(),
                BacklogGrouper.WORKFLOW, IB,
                BacklogGrouper.STEP, Step.CHECK_IN.getName()),
            QUANTITY_BACKLOG_CURRENT,
            0
        ),
        new Photo.Group(
            Map.of(
                BacklogGrouper.DATE_IN, NOW.toString(),
                BacklogGrouper.DATE_OUT, NOW.plus(1, ChronoUnit.DAYS).toString(),
                BacklogGrouper.WORKFLOW, IB,
                BacklogGrouper.STEP, Step.PUT_AWAY.getName()),
            QUANTITY_BACKLOG_CURRENT,
            0
        ),
        new Photo.Group(
            Map.of(
                BacklogGrouper.DATE_IN, TOMORROW.toString(),
                BacklogGrouper.DATE_OUT, TOMORROW.plus(1, ChronoUnit.DAYS).toString(),
                BacklogGrouper.WORKFLOW, IB,
                BacklogGrouper.STEP, Step.SCHEDULED.getName()),
            QUANTITY_BACKLOG_CURRENT,
            0
        ),
        new Photo.Group(
            Map.of(
                BacklogGrouper.DATE_IN, TOMORROW.toString(),
                BacklogGrouper.DATE_OUT, TOMORROW.plus(1, ChronoUnit.DAYS).toString(),
                BacklogGrouper.WORKFLOW, IB,
                BacklogGrouper.STEP, Step.CHECK_IN.getName()),
            QUANTITY_BACKLOG_CURRENT,
            0
        ),
        new Photo.Group(
            Map.of(
                BacklogGrouper.DATE_IN, TOMORROW.toString(),
                BacklogGrouper.DATE_OUT, TOMORROW.plus(1, ChronoUnit.DAYS).toString(),
                BacklogGrouper.WORKFLOW, IB,
                BacklogGrouper.STEP, Step.PUT_AWAY.getName()),
            QUANTITY_BACKLOG_CURRENT,
            0
        )
    ).collect(Collectors.toList()));

    if (transfer) {
      photo.getGroups().addAll(List.of(
          new Photo.Group(
              Map.of(
                  BacklogGrouper.DATE_IN, TODAY.toString(),
                  BacklogGrouper.DATE_OUT, TODAY.plus(1, ChronoUnit.DAYS).toString(),
                  BacklogGrouper.WORKFLOW, IB_TRANSFER,
                  BacklogGrouper.STEP, Step.SCHEDULED.getName()),
              QUANTITY_BACKLOG_CURRENT,
              0
          ),
          new Photo.Group(
              Map.of(
                  BacklogGrouper.DATE_IN, TODAY.toString(),
                  BacklogGrouper.DATE_OUT, TODAY.plus(1, ChronoUnit.DAYS).toString(),
                  BacklogGrouper.WORKFLOW, IB_TRANSFER,
                  BacklogGrouper.STEP, Step.CHECK_IN.getName()),
              QUANTITY_BACKLOG_CURRENT,
              0
          ),
          new Photo.Group(
              Map.of(
                  BacklogGrouper.DATE_IN, TODAY.toString(),
                  BacklogGrouper.DATE_OUT, TODAY.plus(1, ChronoUnit.DAYS).toString(),
                  BacklogGrouper.WORKFLOW, IB_TRANSFER,
                  BacklogGrouper.STEP, Step.PUT_AWAY.getName()),
              QUANTITY_BACKLOG_CURRENT,
              0
          ),
          new Photo.Group(
              Map.of(
                  BacklogGrouper.DATE_IN, NOW.toString(),
                  BacklogGrouper.DATE_OUT, NOW.plus(1, ChronoUnit.DAYS).toString(),
                  BacklogGrouper.WORKFLOW, IB_TRANSFER,
                  BacklogGrouper.STEP, Step.SCHEDULED.getName()),
              QUANTITY_BACKLOG_CURRENT,
              0
          ),
          new Photo.Group(
              Map.of(
                  BacklogGrouper.DATE_IN, NOW.toString(),
                  BacklogGrouper.DATE_OUT, NOW.plus(1, ChronoUnit.DAYS).toString(),
                  BacklogGrouper.WORKFLOW, IB_TRANSFER,
                  BacklogGrouper.STEP, Step.CHECK_IN.getName()),
              QUANTITY_BACKLOG_CURRENT,
              0
          ),
          new Photo.Group(
              Map.of(
                  BacklogGrouper.DATE_IN, NOW.toString(),
                  BacklogGrouper.DATE_OUT, NOW.plus(1, ChronoUnit.DAYS).toString(),
                  BacklogGrouper.WORKFLOW, IB_TRANSFER,
                  BacklogGrouper.STEP, Step.PUT_AWAY.getName()),
              QUANTITY_BACKLOG_CURRENT,
              0
          ),
          new Photo.Group(
              Map.of(
                  BacklogGrouper.DATE_IN, TOMORROW.toString(),
                  BacklogGrouper.DATE_OUT, TOMORROW.plus(1, ChronoUnit.DAYS).toString(),
                  BacklogGrouper.WORKFLOW, IB_TRANSFER,
                  BacklogGrouper.STEP, Step.SCHEDULED.getName()),
              QUANTITY_BACKLOG_CURRENT,
              0
          ),
          new Photo.Group(
              Map.of(
                  BacklogGrouper.DATE_IN, TOMORROW.toString(),
                  BacklogGrouper.DATE_OUT, TOMORROW.plus(1, ChronoUnit.DAYS).toString(),
                  BacklogGrouper.WORKFLOW, IB_TRANSFER,
                  BacklogGrouper.STEP, Step.CHECK_IN.getName()),
              QUANTITY_BACKLOG_CURRENT,
              0
          ),
          new Photo.Group(
              Map.of(
                  BacklogGrouper.DATE_IN, TOMORROW.toString(),
                  BacklogGrouper.DATE_OUT, TOMORROW.plus(1, ChronoUnit.DAYS).toString(),
                  BacklogGrouper.WORKFLOW, IB_TRANSFER,
                  BacklogGrouper.STEP, Step.PUT_AWAY.getName()),
              QUANTITY_BACKLOG_CURRENT,
              0
          )
      ));
    }

    return photo;
  }
}
