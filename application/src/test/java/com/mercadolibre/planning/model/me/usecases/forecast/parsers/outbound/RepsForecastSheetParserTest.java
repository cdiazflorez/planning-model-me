package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound;

import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.BACKLOG_LIMITS;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.MONO_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.MULTI_BATCH_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.MULTI_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.POLYVALENT_BATCH_SORTER;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.POLYVALENT_PACKING;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.POLYVALENT_PACKING_WALL;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.POLYVALENT_PICKING;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.POLYVALENT_WALL_IN;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.WEEK;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessName.PACKING;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessName.PICKING;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessName.WALL_IN;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessName.WAVING;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.BACKLOG_LOWER_LIMIT;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.BACKLOG_UPPER_LIMIT;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastSheet.WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.getMeliSheetFrom;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mercadolibre.planning.model.me.exception.ForecastParsingException;
import com.mercadolibre.planning.model.me.exception.UnmatchedWarehouseException;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.BacklogLimit;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastColumn;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastSheetDto;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessName;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RepsForecastSheetParserTest {

  private static final String INCORRECT_WAREHOUSE_ID = "ERRONEO";

  private static final String VALID_FILE_PATH = "outbound_forecast.xlsx";

  private static final String VALID_FILE_WITH_UNUSED_CONTENT_PATH =
      "outbound_forecast_invalid_content_in_unused_columns.xlsx";

  private static final String INVALID_DATE_PATH = "outbound_forecast_invalid_date.xlsx";

  private static final String INVALID_WEEK_PATH = "outbound_forecast_invalid_week.xlsx";

  private static final String LIMITS_OUT_OF_BOUND_PATH =
      "outbound_forecast_backlog_limits_out_of_bound.xlsx";

  private static final LogisticCenterConfiguration CONF =
      new LogisticCenterConfiguration(TimeZone.getDefault());

  private final RepsForecastSheetParser repsForecastSheetParser = new RepsForecastSheetParser();

  @Test
  @DisplayName("Excel Parsed Ok")
  void parseOk() {
    // GIVEN
    // a valid sheet
    final var sheet = getMeliSheetFrom(WORKERS.getName(), VALID_FILE_PATH);

    // WHEN
    final var forecastSheetDto = repsForecastSheetParser.parse(WAREHOUSE_ID, sheet, CONF);

    // THEN
    thenForecastSheetDtoIsNotNull(forecastSheetDto);
    assertBacklogLimits(forecastSheetDto);
  }

  @Test
  @DisplayName("Excel Parsed Ok When There Is Content In Unused Columns")
  void parseOkWhenThereIsContentInUnusedColumns() {
    // GIVEN
    // a valid sheet
    final var sheet = getMeliSheetFrom(WORKERS.getName(), VALID_FILE_WITH_UNUSED_CONTENT_PATH);

    // WHEN
    final var forecastSheetDto = repsForecastSheetParser.parse(WAREHOUSE_ID, sheet, CONF);

    // THEN
    thenForecastSheetDtoIsNotNull(forecastSheetDto);
  }

  private void thenForecastSheetDtoIsNotNull(final ForecastSheetDto forecastSheetDto) {
    assertNotNull(forecastSheetDto);
    final Map<ForecastColumn, Object> forecastSheetDtoMap = forecastSheetDto.getValues();
    assertEquals("14-2022", forecastSheetDtoMap.get(WEEK));
    assertEquals(42.00, forecastSheetDtoMap.get(MONO_ORDER_DISTRIBUTION));
    assertEquals(33.69, forecastSheetDtoMap.get(MULTI_BATCH_DISTRIBUTION));
    assertEquals(24.31, forecastSheetDtoMap.get(MULTI_ORDER_DISTRIBUTION));
    assertEquals(80.00, forecastSheetDtoMap.get(POLYVALENT_PICKING));
    assertEquals(100.00, forecastSheetDtoMap.get(POLYVALENT_BATCH_SORTER));
    assertEquals(100.00, forecastSheetDtoMap.get(POLYVALENT_WALL_IN));
    assertEquals(100.00, forecastSheetDtoMap.get(POLYVALENT_PACKING));
    assertEquals(100.00, forecastSheetDtoMap.get(POLYVALENT_PACKING_WALL));
  }

  private void assertBacklogLimits(final ForecastSheetDto forecastSheetDto) {
    final var limits = (List<BacklogLimit>) forecastSheetDto.getValues().get(BACKLOG_LIMITS);

    assertBacklogLimitValues(limits.get(0), BACKLOG_LOWER_LIMIT, WAVING, 72);
    assertBacklogLimitValues(limits.get(1), BACKLOG_UPPER_LIMIT, WAVING, 84);

    assertBacklogLimitValues(limits.get(2), BACKLOG_LOWER_LIMIT, PICKING, 96);
    assertBacklogLimitValues(limits.get(3), BACKLOG_UPPER_LIMIT, PICKING, 108);

    assertBacklogLimitValues(limits.get(4), BACKLOG_LOWER_LIMIT, PACKING, 120);
    assertBacklogLimitValues(limits.get(5), BACKLOG_UPPER_LIMIT, PACKING, 132);

    assertBacklogLimitValues(limits.get(6), BACKLOG_LOWER_LIMIT, BATCH_SORTER, 144);
    assertBacklogLimitValues(limits.get(7), BACKLOG_UPPER_LIMIT, BATCH_SORTER, 156);

    assertBacklogLimitValues(limits.get(8), BACKLOG_LOWER_LIMIT, WALL_IN, 168);
    assertBacklogLimitValues(limits.get(9), BACKLOG_UPPER_LIMIT, WALL_IN, 180);

    assertBacklogLimitValues(limits.get(10), BACKLOG_LOWER_LIMIT, PACKING_WALL, 192);
    assertBacklogLimitValues(limits.get(11), BACKLOG_UPPER_LIMIT, PACKING_WALL, -1);
  }

  private void assertBacklogLimitValues(
      final BacklogLimit limit,
      final ForecastProcessType type,
      final ForecastProcessName process,
      final int quantity) {

    assertEquals(type, limit.getType());
    assertEquals(process, limit.getProcessName());
    limit.getData().forEach(data -> assertEquals(quantity, data.getQuantity()));
  }

  @Test
  @DisplayName("Cell value out of range")
  void parseLimitOutRange() {
    // GIVEN
    // a sheet with waving upper limit out of bounds (F12)
    final var sheet = getMeliSheetFrom(WORKERS.getName(), LIMITS_OUT_OF_BOUND_PATH);

    // WHEN
    final var exception =
        assertThrows(
            ForecastParsingException.class,
            () -> repsForecastSheetParser.parse(WAREHOUSE_ID, sheet, CONF));

    // THEN
    final var message = exception.getMessage();
    assertNotNull(message);
    assertTrue(message.contains("F12"));
  }

  @Test
  @DisplayName("Excel parsed with unmatched warehouse error")
  void parseWhenUnmatchWarehouseId() {
    // GIVEN
    // a valid sheet
    final var sheet = getMeliSheetFrom(WORKERS.getName(), VALID_FILE_PATH);

    // WHEN - THEN
    assertThrows(
        UnmatchedWarehouseException.class,
        () -> repsForecastSheetParser.parse(INCORRECT_WAREHOUSE_ID, sheet, CONF));
  }

  @Test
  @DisplayName("Excel parsed with errors in date format")
  void parseFileWithInvalidDateFormat() {
    // GIVEN
    // a sheet with an invalid date (B11: 13/04/2022 03:00)
    final var sheet = getMeliSheetFrom(WORKERS.getName(), INVALID_DATE_PATH);

    // WHEN - THEN
    final var exception =
        assertThrows(
            ForecastParsingException.class,
            () -> repsForecastSheetParser.parse(WAREHOUSE_ID, sheet, CONF));

    // THEN
    final var message = exception.getMessage();
    assertNotNull(message);
    assertTrue(message.contains("B11"));
  }

  @Test
  @DisplayName("Excel parsed with invalid week format")
  void parseFileWithInvalidWeekFormat() {
    // GIVEN
    final var sheet = getMeliSheetFrom(WORKERS.getName(), INVALID_WEEK_PATH);

    // WHEN - THEN
    final var exception =
        assertThrows(
            ForecastParsingException.class,
            () -> repsForecastSheetParser.parse(WAREHOUSE_ID, sheet, CONF));

    // THEN
    final var message = exception.getMessage();
    assertNotNull(exception.getMessage());
    assertTrue(message.contains("Week"));
  }
}
