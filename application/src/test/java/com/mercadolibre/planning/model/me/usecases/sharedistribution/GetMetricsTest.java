package com.mercadolibre.planning.model.me.usecases.sharedistribution;

import com.mercadolibre.planning.model.me.entities.sharedistribution.ShareDistribution;
import com.mercadolibre.planning.model.me.gateways.sharedistribution.ShareDistributionGateway;
import com.mercadolibre.planning.model.me.gateways.sharedistribution.dto.DistributionResponse;
import com.mercadolibre.planning.model.me.utils.DateUtils;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetMetricsTest {

  private static final String WAREHOUSE_ID = "ARTW01";

  @InjectMocks
  private GetMetrics getMetrics;

  @Mock
  private ShareDistributionGateway shareDistributionGateway;

  @Test
  public void getMetricsTest() {

    //GIVEN
    when(shareDistributionGateway.getMetrics(WAREHOUSE_ID)).thenReturn(mockDistribution());
    List<Double> expectedQuantity = Arrays.asList(0.51, 0.23, 0.26);

    ZonedDateTime from = DateUtils.getCurrentUtcDate().truncatedTo(ChronoUnit.DAYS).plusDays(1);
    ZonedDateTime to = from.plusDays(3);


    // WHEN
    List<ShareDistribution> result = getMetrics.execute(WAREHOUSE_ID, from, to);

    // THEN
    result.forEach(s -> {
      assertTrue(expectedQuantity.contains(s.getQuantity()));
      assertTrue(s.getDate().isBefore(to) && (s.getDate().isAfter(from) || s.getDate().isEqual(from)));
    });


  }

  private List<DistributionResponse> mockDistribution() {

    ZonedDateTime to = ZonedDateTime.now().minusDays(1).withHour(23).withMinute(30).withSecond(0).withNano(0);
    ZonedDateTime from = ZonedDateTime.now().minusDays(1).minusMonths(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

    List<DistributionResponse> response = new ArrayList<>();

    while (from.isBefore(to)) {
      response.add(DistributionResponse.builder().sis(45).area("HV-3").warehouseID(WAREHOUSE_ID).cptTime(from).build());
      response.add(DistributionResponse.builder().sis(50).area("RK-0").warehouseID(WAREHOUSE_ID).cptTime(from).build());
      response.add(DistributionResponse.builder().sis(100).area("MZ-0").warehouseID(WAREHOUSE_ID).cptTime(from).build());

      from = from.plusHours(1);
    }

    return response;
  }
}
