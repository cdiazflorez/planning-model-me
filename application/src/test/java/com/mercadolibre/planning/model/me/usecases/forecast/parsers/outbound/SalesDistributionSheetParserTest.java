package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound;

import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastSheet.ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.getMeliSheetFrom;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mercadolibre.planning.model.me.enums.ProcessPath;
import com.mercadolibre.planning.model.me.exception.ForecastParsingException;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistribution;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastSheetDto;
import com.mercadolibre.spreadsheet.MeliSheet;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SalesDistributionSheetParserTest {

  private static final String VALID_FILE_PATH = "outbound_forecast.xlsx";

  private static final String VALID_FILE_PATH_PILOT = "MVP2-outbound-forecast.xlsx";

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
    final ForecastSheetDto forecastSheetDto = salesDistributionSheetParser.parse("ARBA01", repsSheet, CONF);

    // THEN
    assertNotNull(forecastSheetDto);
  }

  @Test
  void parsePilotOk() {
    // GIVEN
    final MeliSheet repsSheet = getMeliSheetFrom(ORDER_DISTRIBUTION.getName(), VALID_FILE_PATH_PILOT);
    // not a test, only a check
    assert repsSheet != null;

    // WHEN
    final ForecastSheetDto forecastSheetDto = salesDistributionSheetParser.parse(WAREHOUSE_ID, repsSheet, CONF);

    var rows = forecastSheetDto.getValues().values().iterator().next();
    @SuppressWarnings("unchecked") var firstRow = ((List<PlanningDistribution>)rows).get(0);

    // THEN
    assertNotNull(forecastSheetDto);
    assertEquals(firstRow.getDateIn(), ZonedDateTime.parse("2022-10-31T03:00Z"));
    assertEquals(firstRow.getDateOut(), ZonedDateTime.parse("2022-10-31T07:00Z"));
    assertEquals(firstRow.getCarrierId(), "webpack");
    assertEquals(firstRow.getServiceId(), "0");
    assertEquals(firstRow.getCanalization(), "web202");
    assertEquals(firstRow.getProcessPath(), ProcessPath.NON_TOT_MONO);
    assertEquals(firstRow.getQuantity(), 237.0);
    assertEquals(firstRow.getQuantityMetricUnit(), "units");
    assertEquals(firstRow.getMetadata(), List.of());
  }

  @Test
  @DisplayName("Excel parsed with errors in date format")
  void parseFileWithInvalidDateFormat() {
    final MeliSheet ordersSheet = getMeliSheetFrom(ORDER_DISTRIBUTION.getName(), INVALID_DATE_FILE_PATH);
    // not a test, only a check
    assert ordersSheet != null;

    assertThrows(ForecastParsingException.class, () -> salesDistributionSheetParser.parse("ARBA01", ordersSheet, CONF));
  }

  @Test
  @DisplayName("Excel with content in unused columns parse OK")
  void parseFileWithContentInUnUsedColumnsOk() {
    // GIVEN
    final MeliSheet repsSheet = getMeliSheetFrom(ORDER_DISTRIBUTION.getName(), INVALID_COLUMN_FILE_PATH);
    // not a test, only a check
    assert repsSheet != null;

    // WHEN
    final ForecastSheetDto forecastSheetDto = salesDistributionSheetParser.parse("ARBA01", repsSheet, CONF);

    // THEN
    assertNotNull(forecastSheetDto);
  }
}
