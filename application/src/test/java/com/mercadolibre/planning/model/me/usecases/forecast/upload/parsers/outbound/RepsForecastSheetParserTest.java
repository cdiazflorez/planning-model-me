package com.mercadolibre.planning.model.me.usecases.forecast.upload.parsers.outbound;

import com.mercadolibre.planning.model.me.exception.ForecastParsingException;
import com.mercadolibre.planning.model.me.exception.UnmatchedWarehouseException;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.dto.ForecastSheetDto;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastColumnName;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.parsers.RepsForecastSheetParser;
import com.mercadolibre.spreadsheet.MeliSheet;
import org.junit.jupiter.api.DisplayName;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepsForecastSheetParserTest {

    private static final String INVALID_FILE_PATH = "forecast_example_invalid_date.xlsx";

	@InjectMocks
    private RepsForecastSheetParser repsForecastSheetParser;

    @Mock
    private LogisticCenterGateway logisticCenterGateway;

    private MeliSheet repsSheet;
    private ForecastSheetDto forecastSheetDto;
    private static final String incorrectWarehouseId = "ERRONEO";

    @Test
    @DisplayName("Excel Parsed Ok")
    void parseOk() {
        //GIVEN
        givenAnCorrectConfigurationAndMeliSheetBy();

        //WHEN
        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
                .thenReturn(new LogisticCenterConfiguration(TimeZone.getDefault()));
        whenExcelIsParsedBy(WAREHOUSE_ID);
        //THEN
        thenForecastSheetDtoIsNotNull();
    }

    @Test
    @DisplayName("Excel parsed with unmatched warehouse error")
    void parseWhenUnmatchWarehouseId() {
        //GIVEN
        givenAnCorrectConfigurationAndMeliSheetBy();
        //WHEN - THEN
        assertThrows(UnmatchedWarehouseException.class,
                () -> whenExcelIsParsedBy(incorrectWarehouseId));
    }

    private void givenAnCorrectConfigurationAndMeliSheetBy() {
        repsSheet = getMeliSheetFromTestFile(WORKERS.getName());
    }

    private void whenExcelIsParsedBy(String warehouseId) {
        forecastSheetDto = repsForecastSheetParser.parse(warehouseId, repsSheet);
    }

    private void thenForecastSheetDtoIsNotNull() {
        assertNotNull(forecastSheetDto);
        final Map<ForecastColumnName, Object> forecastSheetDtoMap = forecastSheetDto.getValues();
        assertEquals(50.11, forecastSheetDtoMap.get(MONO_ORDER_DISTRIBUTION));
        assertEquals(30.56, forecastSheetDtoMap.get(MULTI_BATCH_DISTRIBUTION));
        assertEquals(19.33, forecastSheetDtoMap.get(MULTI_ORDER_DISTRIBUTION));


    }
    
    @Test
    @DisplayName("Excel parsed with errors in date format")
    void parseFileWithInvalidDateFormat() {
    	givenAnExcelFileWithInvalidDate();
    	assertThrows(ForecastParsingException.class, ()->whenExcelIsParsedBy(WAREHOUSE_ID));
    }

	private void givenAnExcelFileWithInvalidDate() {
		when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
        .thenReturn(new LogisticCenterConfiguration(TimeZone.getDefault()));
		repsSheet = getMeliSheetFromTestFile(WORKERS.getName(),INVALID_FILE_PATH);
		
	}

}
