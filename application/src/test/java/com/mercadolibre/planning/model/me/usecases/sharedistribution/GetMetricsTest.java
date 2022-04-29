package com.mercadolibre.planning.model.me.usecases.sharedistribution;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.entities.sharedistribution.ShareDistribution;
import com.mercadolibre.planning.model.me.gateways.sharedistribution.ShareDistributionGateway;
import com.mercadolibre.planning.model.me.gateways.sharedistribution.dto.DistributionElement;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Test of GetMetrics. */
@ExtendWith(MockitoExtension.class)
public class GetMetricsTest {

  private static final String WAREHOUSE_ID = "ARTW01";

  @InjectMocks
  private GetMetrics getMetrics;

  @Mock
  private ShareDistributionGateway shareDistributionGateway;

  @Test
  public void metricsTest() {

    Instant from = Instant.now().truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS);
    Instant to = from.plus(3, ChronoUnit.DAYS);

    Instant dateToQuery = from.minus(24, ChronoUnit.HOURS);
    Instant dateFromQuery = dateToQuery.minus(28, ChronoUnit.DAYS);
    //GIVEN
    when(shareDistributionGateway.getMetrics(WAREHOUSE_ID, dateFromQuery, dateToQuery)).thenReturn(mockDistribution());
    List<Double> expectedQuantity = Arrays.asList(0.51, 0.23, 0.26);

    // WHEN
    List<ShareDistribution> result = getMetrics.execute(WAREHOUSE_ID, from, to);

    // THEN
    result.forEach(s -> {
      assertTrue(expectedQuantity.contains(s.getQuantity()));
      Instant date = s.getDate().toInstant();
      assertTrue(date.isBefore(to) && (date.isAfter(from) || date.equals(from)));
    });


  }

  private List<DistributionElement> mockDistribution() {

    ZonedDateTime to = ZonedDateTime.now().minusDays(1).withHour(23).withMinute(30).withSecond(0).withNano(0);
    ZonedDateTime from = ZonedDateTime.now().minusDays(1).minusMonths(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

    List<DistributionElement> response = new ArrayList<>();

    while (from.isBefore(to)) {
      response.add(DistributionElement.builder().sis(45).area("HV-3").warehouseID(WAREHOUSE_ID).cptTime(from).build());
      response.add(DistributionElement.builder().sis(50).area("RK-0").warehouseID(WAREHOUSE_ID).cptTime(from).build());
      response.add(DistributionElement.builder().sis(100).area("MZ-0").warehouseID(WAREHOUSE_ID).cptTime(from).build());

      from = from.plusHours(1);
    }

    return response;
  }
}
