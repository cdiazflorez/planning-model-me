package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound;

import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastSheet.ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.getMeliSheetFrom;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

  private static final LogisticCenterConfiguration CONF = new LogisticCenterConfiguration(TimeZone.getTimeZone("UTC"));

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

    var rows = forecastSheetDto.getValues().values().stream().map(item -> (List<PlanningDistribution>) item)
        .findFirst().get();
    var firstRow = rows.stream().filter(item -> ZonedDateTime.parse("2022-10-31T00:00Z").equals(item.getDateIn())
        && ZonedDateTime.parse("2022-10-31T04:00Z").equals(item.getDateOut())).collect(Collectors.toList());

    var secondRow = rows.stream().filter(item -> ZonedDateTime.parse("2022-10-30T00:00Z").equals(item.getDateIn())
        && ZonedDateTime.parse("2022-10-30T04:00Z").equals(item.getDateOut())).collect(Collectors.toList());

    // THEN
    assertNotNull(forecastSheetDto);
    assertEquals(firstRow.size(), 1);
    assertEquals(firstRow.get(0).getCarrierId(), "webpack");
    assertEquals(firstRow.get(0).getServiceId(), "0");
    assertEquals(firstRow.get(0).getCanalization(), "web202");
    assertEquals(firstRow.get(0).getProcessPath(), ProcessPath.NON_TOT_MONO);
    assertEquals(firstRow.get(0).getQuantity(), 237.05);
    assertEquals(secondRow.get(0).getQuantity(), 164.21);
    assertEquals(firstRow.get(0).getQuantityMetricUnit(), "units");
    assertEquals(firstRow.get(0).getMetadata(), List.of());
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
