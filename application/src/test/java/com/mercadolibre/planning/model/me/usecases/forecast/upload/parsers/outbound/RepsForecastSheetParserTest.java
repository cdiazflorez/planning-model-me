package com.mercadolibre.planning.model.me.usecases.forecast.upload.parsers.outbound;

import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.dto.ForecastSheetDto;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.parsers.RepsForecastSheetParser;
import com.mercadolibre.spreadsheet.MeliSheet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.TimeZone;

import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastSheet.WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.getMeliSheetFromTestFile;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RepsForecastSheetParserTest {

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
    }

}
