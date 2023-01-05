package com.mercadolibre.planning.model.me.usecases.backlog;

import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.ZoneOffset.UTC;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogLastPhotoRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogPhotosRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogScheduled;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Indicator;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GetBacklogScheduledTest {

    private static final int AMOUNT_TO_ADD_MINUTES = 5;
    private static final int QUANTITY_BACKLOG = 500;
    private static final int QUANTITY_BACKLOG_CURRENT = 275;

    @InjectMocks
    private GetBacklogScheduled getBacklogScheduled;

    @Mock
    private BacklogApiGateway backlogGateway;

    @Mock
    private LogisticCenterGateway logisticCenterGateway;

    private Instant now;
    private Instant today;
    @BeforeEach
    public void init() {
        now = Instant.now();
        today = ZonedDateTime.ofInstant(now, UTC)
                .withZoneSameInstant(TimeZone.getDefault().toZoneId())
                .truncatedTo(ChronoUnit.DAYS)
                .toInstant();
    }

    @Test
    public void getBacklogScheduledTest() {
        //GIVEN

        final Instant photoDateTo = today.plus(AMOUNT_TO_ADD_MINUTES, ChronoUnit.MINUTES);

        //WHEN
        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
                .thenReturn(new LogisticCenterConfiguration(TimeZone.getDefault()));
        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
                .thenReturn(new LogisticCenterConfiguration(TimeZone.getDefault()));

        //first photo of day
      when(backlogGateway.getPhotos(any(BacklogPhotosRequest.class))).thenReturn(responseGetBacklog(true));

      when(backlogGateway.getLastPhoto(any(BacklogLastPhotoRequest.class))).thenReturn(responseGetCurrentBacklog(true));

        final Map<String, BacklogScheduled> response = getBacklogScheduled.execute(WAREHOUSE_ID, now);

        //expected
        BacklogScheduled expectedBacklogInbound = new BacklogScheduled(
                Indicator.builder().units(500).build(),
                Indicator.builder().units(275).build(),
                Indicator.builder().units(0).build(),
                Indicator.builder().units(225).percentage(-0.45).build()
        );

        BacklogScheduled expectedBacklogInboundTransfer = new BacklogScheduled(
            Indicator.builder().units(650).build(),
            Indicator.builder().units(150).build(),
            Indicator.builder().units(0).build(),
            Indicator.builder().units(500).percentage(-0.77).build()
        );

        BacklogScheduled expectedTotalBacklog = new BacklogScheduled(
            Indicator.builder().units(1150).build(),
            Indicator.builder().units(425).build(),
            Indicator.builder().units(0).build(),
            Indicator.builder().units(725).percentage(-0.63).build()
        );

        //verify
        Assertions.assertEquals(expectedBacklogInbound, response.get("inbound"));
        Assertions.assertEquals(expectedBacklogInboundTransfer, response.get("inbound_transfer"));
        Assertions.assertEquals(expectedTotalBacklog, response.get("total"));
    }

    @Test
    public void testBacklogScheduledWithoutTransferBacklog() {
        //WHEN
        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
            .thenReturn(new LogisticCenterConfiguration(TimeZone.getDefault()));
        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
            .thenReturn(new LogisticCenterConfiguration(TimeZone.getDefault()));

        //first photo of day
      when(backlogGateway.getPhotos(any(BacklogPhotosRequest.class))).thenReturn(responseGetBacklog(false));

      when(backlogGateway.getLastPhoto(any(BacklogLastPhotoRequest.class))).thenReturn(null);

        final Map<String, BacklogScheduled> response = getBacklogScheduled.execute(WAREHOUSE_ID, now);

        //expected
        BacklogScheduled expectedBacklogSeller = new BacklogScheduled(
            Indicator.builder().units(500).build(),
            Indicator.builder().units(0).build(),
            Indicator.builder().units(0).build(),
            Indicator.builder().units(500).percentage(-1.0).build()
        );

        BacklogScheduled expectedBacklogTransfer = new BacklogScheduled(
            Indicator.builder().units(0).build(),
            Indicator.builder().units(0).build(),
            Indicator.builder().units(0).build(),
            Indicator.builder().units(0).percentage(0.0).build()
        );

      //verify
      Assertions.assertEquals(expectedBacklogSeller, response.get("inbound"));
      Assertions.assertEquals(expectedBacklogTransfer, response.get("inbound_transfer"));
      Assertions.assertEquals(response.get("inbound"), response.get("total"));
    }

  private List<Photo> responseGetBacklog(boolean transfer) {

    return transfer
        ? List.of(new Photo(
        Instant.now(),
        List.of(
            new Photo.Group(
                Map.of(BacklogGrouper.DATE_IN, today.toString(), BacklogGrouper.WORKFLOW, "inbound"),
                QUANTITY_BACKLOG,
                0
            ),
            new Photo.Group(
                Map.of(BacklogGrouper.DATE_IN, today.toString(), BacklogGrouper.WORKFLOW, "INBOUND-TRANSFER"),
                QUANTITY_BACKLOG + 150,
                0
            )
        )
    ))
        : List.of(new Photo(
        Instant.now(),
        List.of(
            new Photo.Group(
                Map.of(BacklogGrouper.DATE_IN, today.toString(), BacklogGrouper.WORKFLOW, "inbound"),
                QUANTITY_BACKLOG,
                0
            )
        )
    ));
  }

  private Photo responseGetCurrentBacklog(boolean transfer) {
    return new Photo(Instant.now(),
        transfer
            ? List.of(new Photo.Group(
                Map.of(BacklogGrouper.DATE_IN, today.toString(), BacklogGrouper.WORKFLOW, "inbound"),
                QUANTITY_BACKLOG_CURRENT,
                0
            ),
            new Photo.Group(Map.of(BacklogGrouper.DATE_IN, today.toString(), BacklogGrouper.WORKFLOW, "INBOUND-TRANSFER"),
                QUANTITY_BACKLOG_CURRENT - 125,
                0))
            : List.of(new Photo.Group(
            Map.of(BacklogGrouper.DATE_IN, today.toString(), BacklogGrouper.WORKFLOW, "inbound"),
            QUANTITY_BACKLOG_CURRENT,
            0
        ))
    );
  }
}
