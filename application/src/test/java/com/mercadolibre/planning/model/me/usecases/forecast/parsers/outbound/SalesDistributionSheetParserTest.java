package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound;

import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastSheet.ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.getMeliSheetFrom;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mercadolibre.planning.model.me.exception.ForecastParsingException;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastSheetDto;
import com.mercadolibre.spreadsheet.MeliSheet;
import java.util.TimeZone;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SalesDistributionSheetParserTest {

  private static final String VALID_FILE_PATH = "outbound_forecast.xlsx";

  private static final String INVALID_DATE_FILE_PATH = "outbound_forecast_invalid_date.xlsx";

  private static final String INVALID_COLUMN_FILE_PATH = "outbound_forecast_invalid_content_in_unused_columns.xlsx";

  private static final LogisticCenterConfiguration CONF = new LogisticCenterConfiguration(TimeZone.getDefault());

  private final SalesDistributionSheetParser salesDistributionSheetParser = new SalesDistributionSheetParser();

  @Test
  void parseOk() {
    // GIVEN
    final MeliSheet repsSheet = getMeliSheetFrom(ORDER_DISTRIBUTION.getName(), VALID_FILE_PATH);
    // not a test, only a check
    assert repsSheet != null;

    // WHEN
    final ForecastSheetDto forecastSheetDto = salesDistributionSheetParser.parse(WAREHOUSE_ID, repsSheet, CONF);

    // THEN
    assertNotNull(forecastSheetDto);
  }

  @Test
  @DisplayName("Excel parsed with errors in date format")
  void parseFileWithInvalidDateFormat() {
    final MeliSheet ordersSheet = getMeliSheetFrom(ORDER_DISTRIBUTION.getName(), INVALID_DATE_FILE_PATH);
    // not a test, only a check
    assert ordersSheet != null;

    assertThrows(ForecastParsingException.class, () -> salesDistributionSheetParser.parse(WAREHOUSE_ID, ordersSheet, CONF));
  }

  @Test
  @DisplayName("Excel with content in unused columns parse OK")
  void parseFileWithContentInUnUsedColumnsOk() {
    // GIVEN
    final MeliSheet repsSheet = getMeliSheetFrom(ORDER_DISTRIBUTION.getName(), INVALID_COLUMN_FILE_PATH);
    // not a test, only a check
    assert repsSheet != null;

    // WHEN
    final ForecastSheetDto forecastSheetDto = salesDistributionSheetParser.parse(WAREHOUSE_ID, repsSheet, CONF);

    // THEN
    assertNotNull(forecastSheetDto);
  }
}
