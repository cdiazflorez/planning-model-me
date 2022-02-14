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

    private static final int AMOUNT_TO_ADD_MINUTES = 5;
    private static final int AMOUNT_TO_ADD_DAYS = 1;
    private static final int QUANTITY_BACKLOG = 500;
    private static final int QUANTITY_BACKLOG_CURRENT = 225;

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
                        .withWorkflows(List.of("inbound"))
                        .withGroupingFields(List.of("date_in"))
                        .withDateInRange(today, today.plus(AMOUNT_TO_ADD_DAYS, ChronoUnit.DAYS))
        )).thenReturn(responseGetBacklog());

        when(backlogGateway.getCurrentBacklog(
                new BacklogCurrentRequest(WAREHOUSE_ID)
                        .withWorkflows(List.of("inbound"))
                        .withSteps(List.of("SCHEDULED"))
                        .withDateInRange(today, now)
                        .withGroupingFields(List.of("process"))
        )).thenReturn(responseGetCurrentBacklog());

        BacklogScheduled response =
                getBacklogScheduled.execute(WAREHOUSE_ID, now);

        //expected
        BacklogScheduled expectedBacklogScheduled = new BacklogScheduled(
                Indicator.builder().units(500).build(),
                Indicator.builder().units(275).build(),
                Indicator.builder().units(0).build(),
                Indicator.builder().units(225).percentage(-0.45).build()
        );

        //verify
        Assertions.assertEquals(expectedBacklogScheduled, response);


    }

    private List<Consolidation> responseGetBacklog() {
        return List.of(new Consolidation(Instant.now(), Map.of("date_in", today.toString()), QUANTITY_BACKLOG));
    }

    private List<Consolidation> responseGetCurrentBacklog() {
        return List.of(new Consolidation(Instant.now(), Map.of("date_in", today.toString()), QUANTITY_BACKLOG_CURRENT));
    }

}
