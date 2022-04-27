package com.mercadolibre.planning.model.me.usecases.sharedistribution;

import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.entities.sharedistribution.ShareDistribution;
import com.mercadolibre.planning.model.me.gateways.entity.EntityGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SaveUnitsResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.utils.DateUtils;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Test of SaveShareDistribution. */
@ExtendWith(MockitoExtension.class)
public class SaveShareDistributionTest {

  private static final String WH = "ARTW01";

  private static final String WHP = "ARBA01";

  private static final String WHC = "COCU01";

  private static final String METRIC_UNIT = "PERCENTAGE";

  private static final String PROCESS = "PICKING";

  private static final int DAYS = 3;

  @Mock
  private EntityGateway entityGateway;

  @Mock
  private GetMetrics getMetrics;

  @InjectMocks
  private SaveShareDistribution saveShareDistribution;

  @Test
  public void testExecute() {

    //GIVEN

    List<ShareDistribution> shareDistributionList = List.of(
        ShareDistribution.builder().date(ZonedDateTime.now()).logisticCenterId(WH).area("MZ-0").processName(PROCESS).quantity(0.2)
            .quantityMetricUnit(METRIC_UNIT).build());
    List<ShareDistribution> shareDistributionList2 = new ArrayList<>();
    List<ShareDistribution> shareDistributionList3 = List.of(
        ShareDistribution.builder().date(ZonedDateTime.now()).logisticCenterId(WH).area("MZ-1").processName(PROCESS).quantity(0.3)
            .quantityMetricUnit(METRIC_UNIT).build());

    ZonedDateTime now = DateUtils.getCurrentUtcDate();
    Instant dateFrom = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
    Instant dateTo = dateFrom.plus(DAYS, ChronoUnit.DAYS);

    when(getMetrics.execute(WH, dateFrom, dateTo)).thenReturn(shareDistributionList);
    when(getMetrics.execute(WHP, dateFrom, dateTo)).thenReturn(shareDistributionList2);
    when(getMetrics.execute(WHC, dateFrom, dateTo)).thenReturn(shareDistributionList3);
    when(entityGateway.saveShareDistribution(shareDistributionList, Workflow.FBM_WMS_OUTBOUND)).thenReturn(
        SaveUnitsResponse.builder().response("Successfully").quantitySave(1).warehouseId(WH).build());
    when(entityGateway.saveShareDistribution(shareDistributionList3, Workflow.FBM_WMS_OUTBOUND)).thenAnswer(invocation -> {
      throw new IllegalArgumentException();
    });


    List<String> warehouses = Arrays.asList(WH, WHP, WHC);
    //WHEN
    List<SaveUnitsResponse> result = saveShareDistribution.execute(warehouses,null, DAYS, Instant.now());


    //THEN
    Assertions.assertNotNull(result);


  }


}
