package com.mercadolibre.planning.model.me.usecases.forecast.upload.parsers.outbound;

import com.mercadolibre.planning.model.me.exception.ForecastParsingException;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.dto.ForecastSheetDto;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.parsers.SalesDistributionSheetParser;
import com.mercadolibre.spreadsheet.MeliSheet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.TimeZone;

import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastSheet.ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.getMeliSheetFrom;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SalesDistributionSheetParserTest {

    private static final String VALID_FILE_PATH = "forecast_example.xlsx";
    private static final String INVALID_DATE_FILE_PATH = "forecast_example_invalid_date.xlsx";
    private static final String INVALID_COLUMN_FILE_PATH =
            "forecast_example_invalid_content_in_unused_columns.xlsx";

    @InjectMocks
    private SalesDistributionSheetParser salesDistributionSheetParser;

    @Mock
    private LogisticCenterGateway logisticCenterGateway;

    private MeliSheet ordersSheet;

    @Test
    void parseOk() {
        // GIVEN
        final MeliSheet repsSheet = getMeliSheetFrom(ORDER_DISTRIBUTION.getName(), VALID_FILE_PATH);
        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
                .thenReturn(new LogisticCenterConfiguration(TimeZone.getDefault()));

        // WHEN
        final ForecastSheetDto forecastSheetDto = salesDistributionSheetParser.parse(WAREHOUSE_ID,
                repsSheet);

        // THEN
        assertNotNull(forecastSheetDto);
    }

    @Test
    @DisplayName("Excel parsed with errors in date format")
    void parseFileWithInvalidDateFormat() {
        givenAnExcelFileWithInvalidDate();
        assertThrows(ForecastParsingException.class,
                () -> salesDistributionSheetParser.parse(WAREHOUSE_ID, ordersSheet));
    }

    private void givenAnExcelFileWithInvalidDate() {
        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
                .thenReturn(new LogisticCenterConfiguration(TimeZone.getDefault()));
        ordersSheet = getMeliSheetFrom(ORDER_DISTRIBUTION.getName(), INVALID_DATE_FILE_PATH);

    }

    @Test
    @DisplayName("Excel with content in unused columns parse OK")
    void parseFileWithContentInUnUsedColumnsOk() {
        // GIVEN
        final MeliSheet repsSheet =
                getMeliSheetFrom(ORDER_DISTRIBUTION.getName(), INVALID_COLUMN_FILE_PATH);

        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
                .thenReturn(new LogisticCenterConfiguration(TimeZone.getDefault()));

        // WHEN
        final ForecastSheetDto forecastSheetDto = salesDistributionSheetParser.parse(WAREHOUSE_ID,
                repsSheet);

        // THEN
        assertNotNull(forecastSheetDto);
    }
}
