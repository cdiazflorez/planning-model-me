package com.mercadolibre.planning.model.me.usecases.sharedistribution;

import com.mercadolibre.planning.model.me.entities.sharedistribution.ShareDistribution;
import com.mercadolibre.planning.model.me.gateways.entity.EntityGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SaveUnitsResponse;
import com.mercadolibre.planning.model.me.utils.DateUtils;
import org.assertj.core.internal.bytebuddy.implementation.bytecode.Throw;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SaveShareDistributionTest {

    private static final String WH = "ARTW01";
    private static final String WHP = "ARBA01";
    private static final String WHC = "COCU01";
    private static final int DAYS = 3;
    @Mock
    private EntityGateway entityGateway;

    @Mock
    private GetMetrics getMetrics;

    @InjectMocks
    SaveShareDistribution saveShareDistribution;

    @Test
    public void testExecute(){

        //GIVEN

        List<ShareDistribution> shareDistributionList = List.of(ShareDistribution.builder().date(ZonedDateTime.now()).logisticCenterId(WH).area("MZ-0").processName("PICKING").quantity(0.2).quantityMetricUnit("PERCENTAJE").build());
        List<ShareDistribution> shareDistributionList2 = new ArrayList<>();
        List<ShareDistribution> shareDistributionList3 = List.of(ShareDistribution.builder().date(ZonedDateTime.now()).logisticCenterId(WH).area("MZ-1").processName("PICKING").quantity(0.3).quantityMetricUnit("PERCENTAJE").build());

        ZonedDateTime now = DateUtils.getCurrentUtcDate();
        ZonedDateTime dateFrom = now.plusDays(1).truncatedTo(ChronoUnit.DAYS);
        ZonedDateTime dateTo = dateFrom.plusDays(DAYS);

        when(getMetrics.execute(WH, dateFrom, dateTo)).thenReturn(shareDistributionList);
        when(getMetrics.execute(WHP, dateFrom, dateTo)).thenReturn(shareDistributionList2);
        when(getMetrics.execute(WHC, dateFrom, dateTo)).thenReturn(shareDistributionList3);
        when(entityGateway.saveShareDistribution(shareDistributionList)).thenReturn( SaveUnitsResponse.builder().response("Successfully").quantitySave(1).warehouseId(WH).build());
        when(entityGateway.saveShareDistribution(shareDistributionList3)).thenAnswer(invocation -> {
            throw new IllegalArgumentException();
        });



        List<String> warehouses = Arrays.asList(WH,WHP,WHC);
        //WHEN
        List<SaveUnitsResponse>  result = saveShareDistribution.execute(warehouses,dateFrom,dateTo);



        //THEN
        Assertions.assertNotNull(result);


    }



}
