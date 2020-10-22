package com.mercadolibre.planning.model.me.usecases.forecast.upload.parsers.outbound;

import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.dto.ForecastSheetDto;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastColumnName;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.parsers.RepsForecastSheetParser;
import com.mercadolibre.spreadsheet.MeliSheet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.TimeZone;

import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastColumnName.MONO_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastColumnName.MULTI_BATCH_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastColumnName.MULTI_ORDER_DISTRIBUTION;

import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastSheet.WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.getMeliSheetFromTestFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepsForecastSheetParserTest {

    @InjectMocks
    private RepsForecastSheetParser repsForecastSheetParser;

    @Mock
    private LogisticCenterGateway logisticCenterGateway;

    @Test
    void parseOk() {
        // GIVEN
        final MeliSheet repsSheet = getMeliSheetFromTestFile(WORKERS.getName());
        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
                .thenReturn(new LogisticCenterConfiguration(TimeZone.getDefault()));

        // WHEN
        final ForecastSheetDto forecastSheetDto =
                repsForecastSheetParser.parse(WAREHOUSE_ID, repsSheet);



        // THEN
        assertNotNull(forecastSheetDto);
        final Map<ForecastColumnName, Object> forecastSheetDtoMap = forecastSheetDto.getValues();
        assertEquals(58L, forecastSheetDtoMap.get(MONO_ORDER_DISTRIBUTION));
        assertEquals(22L, forecastSheetDtoMap.get(MULTI_BATCH_DISTRIBUTION));
        assertEquals(20L, forecastSheetDtoMap.get(MULTI_ORDER_DISTRIBUTION));


    }

}
