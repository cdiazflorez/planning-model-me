package com.mercadolibre.planning.model.me.usecases.backlog;

import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.ZoneOffset.UTC;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogCurrentRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogScheduled;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Indicator;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
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

    private static final String WORKFLOW_KEY = "workflow";
    private static final String DATE_IN_KEY = "date_in";
    private static final int AMOUNT_TO_ADD_MINUTES = 5;
    private static final int AMOUNT_TO_ADD_DAYS = 1;
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
        when(backlogGateway.getBacklog(
                new BacklogRequest(WAREHOUSE_ID, today, photoDateTo)
                        .withWorkflows(List.of("inbound", "INBOUND-TRANSFER"))
                        .withGroupingFields(List.of(DATE_IN_KEY, WORKFLOW_KEY))
                        .withSteps(List.of("SCHEDULED"))
                        .withDateInRange(today, today.plus(AMOUNT_TO_ADD_DAYS, ChronoUnit.DAYS))
        )).thenReturn(responseGetBacklog());

        when(backlogGateway.getCurrentBacklog(
                new BacklogCurrentRequest(WAREHOUSE_ID)
                        .withWorkflows(List.of("inbound", "INBOUND-TRANSFER"))
                        .withSteps(List.of("CHECK_IN", "PUT_AWAY", "FINISHED"))
                        .withDateInRange(today, now)
                        .withGroupingFields(List.of("process", WORKFLOW_KEY))
        )).thenReturn(responseGetCurrentBacklog());

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
        //GIVEN
        final Instant photoDateTo = today.plus(AMOUNT_TO_ADD_MINUTES, ChronoUnit.MINUTES);

        //WHEN
        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
            .thenReturn(new LogisticCenterConfiguration(TimeZone.getDefault()));
        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
            .thenReturn(new LogisticCenterConfiguration(TimeZone.getDefault()));

        //first photo of day
        when(backlogGateway.getBacklog(
            new BacklogRequest(WAREHOUSE_ID, today, photoDateTo)
                .withWorkflows(List.of("inbound", "INBOUND-TRANSFER"))
                .withGroupingFields(List.of(DATE_IN_KEY, WORKFLOW_KEY))
                .withSteps(List.of("SCHEDULED"))
                .withDateInRange(today, today.plus(AMOUNT_TO_ADD_DAYS, ChronoUnit.DAYS))
        )).thenReturn(responseGetBacklog().subList(0, 1));

        when(backlogGateway.getCurrentBacklog(
            new BacklogCurrentRequest(WAREHOUSE_ID)
                .withWorkflows(List.of("inbound", "INBOUND-TRANSFER"))
                .withSteps(List.of("CHECK_IN", "PUT_AWAY", "FINISHED"))
                .withDateInRange(today, now)
                .withGroupingFields(List.of("process", WORKFLOW_KEY))
        )).thenReturn(responseGetCurrentBacklog().subList(0, 1));

        final Map<String, BacklogScheduled> response = getBacklogScheduled.execute(WAREHOUSE_ID, now);

        //expected
        BacklogScheduled expectedBacklogSeller = new BacklogScheduled(
            Indicator.builder().units(500).build(),
            Indicator.builder().units(275).build(),
            Indicator.builder().units(0).build(),
            Indicator.builder().units(225).percentage(-0.45).build()
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

    private List<Consolidation> responseGetBacklog() {
        return List.of(
            new Consolidation(
                Instant.now(),
                Map.of(DATE_IN_KEY, today.toString(), WORKFLOW_KEY, "inbound"),
                QUANTITY_BACKLOG,
                true
            ),
            new Consolidation(
                Instant.now(),
                Map.of(DATE_IN_KEY, today.toString(), WORKFLOW_KEY, "INBOUND-TRANSFER"),
                QUANTITY_BACKLOG + 150,
                true
            )
        );
    }

    private List<Consolidation> responseGetCurrentBacklog() {
        return List.of(
            new Consolidation(
                Instant.now(),
                Map.of(DATE_IN_KEY, today.toString(), WORKFLOW_KEY, "inbound"),
                QUANTITY_BACKLOG_CURRENT,
                true
            ),
            new Consolidation(
                Instant.now(),
                Map.of(DATE_IN_KEY, today.toString(), WORKFLOW_KEY, "INBOUND-TRANSFER"),
                QUANTITY_BACKLOG_CURRENT - 125,
                true
            )
        );
    }
}
