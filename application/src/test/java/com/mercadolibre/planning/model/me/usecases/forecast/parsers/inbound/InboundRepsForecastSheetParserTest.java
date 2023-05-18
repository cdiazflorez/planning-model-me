package com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound;

import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.HEADCOUNT_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.INBOUND_CHECKIN_PRODUCTIVITY_POLYVALENCES;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.INBOUND_PUTAWAY_PRODUCTIVITY_POLIVALENCES;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.INBOUND_RECEIVING_PRODUCTIVITY_POLYVALENCES;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.PROCESSING_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.VERSION;
import static com.mercadolibre.planning.model.me.utils.TestUtils.CHECK_IN_PROCESS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.PUT_AWAY_PROCESS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.RECEIVING_PROCESS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.getMeliSheetFrom;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mercadolibre.planning.model.me.exception.ForecastParsingException;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.HeadcountProductivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingDistribution;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastSheetDto;
import com.mercadolibre.spreadsheet.MeliSheet;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InboundRepsForecastSheetParserTest {

  private static final String VALID_FILE_PATH = "inbound_planning_ok.xlsx";

  private static final String VALID_FILE_PATH_V2 = "inbound_planning_v2_ok.xlsx";

  private static final String ERRONEOUS_FILE_PATH = "inbound_planning_with_errors.xlsx";

  private static final int FIRST_VERSION = 1;

  private static final int SECOND_VERSION = 2;

  private static final LogisticCenterConfiguration CONF =
      new LogisticCenterConfiguration(TimeZone.getTimeZone("UTC"));

  private static final ZonedDateTime FIRST_DATE = ZonedDateTime.parse("2021-11-07T00:00Z", ISO_OFFSET_DATE_TIME);

  private static final ZonedDateTime FIRST_DATE_FOR_RECEIVING = ZonedDateTime.parse("2021-11-07T23:00Z", ISO_OFFSET_DATE_TIME);

  private final InboundRepsForecastSheetParser parser = new InboundRepsForecastSheetParser();

  private static Stream<Arguments> provideFileForParsing() {
    return Stream.of(
        Arguments.of(VALID_FILE_PATH, false),
        Arguments.of(VALID_FILE_PATH_V2, true)
    );
  }

  @DisplayName("Excel Parsed Ok")
  @ParameterizedTest
  @MethodSource("provideFileForParsing")
  void parseOk(final String file, final boolean isLastVersion) {
    // GIVEN
    final MeliSheet repsSheet = getMeliSheetFrom(parser.name(), file);

    // WHEN
    final ForecastSheetDto result = parser.parse("ARBA01", repsSheet, CONF);

    // THEN
    assertSuccessResults(result, isLastVersion);
  }

  @Test
  @DisplayName("Excel With Errors")
  void errors() {
    // GIVEN
    final MeliSheet repsSheet = getMeliSheetFrom(parser.name(), ERRONEOUS_FILE_PATH);

    // WHEN
    final ForecastParsingException exception = assertThrows(ForecastParsingException.class,
        () -> parser.parse("ARBA01", repsSheet, CONF));

    final ForecastParsingException exceptionWarehouse = assertThrows(ForecastParsingException.class,
        () -> parser.parse("ARTW01", repsSheet, CONF));

    // THEN
    assertNotNull(exception.getMessage());
    assertNotNull(exceptionWarehouse.getMessage());

    final String expectedMessage =
        "Error while trying to parse cell (D12) for sheet: Plan de staffing.\n"
            + "Error while trying to parse cell (E15) for sheet: Plan de staffing.\n"
            + "Error while trying to parse cell (B27) for sheet: Plan de staffing.\n"
            + "Error while trying to parse cell (K32) for sheet: Plan de staffing";

    assertEquals(expectedMessage, exception.getMessage());
    assertTrue(exceptionWarehouse.getMessage().contains("Warehouse id ARTW01 is different from warehouse id ARBA01 from file.\n"));
  }

  private void assertSuccessResults(final ForecastSheetDto result, final boolean isLastVersion) {
    assertNotNull(result);
    assertEquals("Plan de staffing", result.getSheetName());
    assertEquals(8, result.getValues().size());
    final int version = (int) result.getValues().get(VERSION);

    // PROCESSING DISTRIBUTIONS
    final var processingDistributions = (List<ProcessingDistribution>) result.getValues()
        .get(PROCESSING_DISTRIBUTION);

    assertNotNull(processingDistributions);

    final var receivingTarget = processingDistributions.get(0);
    assertEquals("receiving", receivingTarget.getProcessName());
    assertEquals(168, receivingTarget.getData().size());

    final var firstRow = receivingTarget.getData().get(0);
    assertEquals(FIRST_DATE, firstRow.getDate());
    assertEquals(0, firstRow.getQuantity());

    // PRODUCTIVITY
    final var productivities = (List<HeadcountProductivity>) result.getValues()
        .get(HEADCOUNT_PRODUCTIVITY);

    assertNotNull(productivities);

    if (isLastVersion) {

      assertEquals(SECOND_VERSION, version);

      assertEquals(20, processingDistributions.size());

      assertEquals(3, productivities.size());

      final var receivingProductivities = productivities.get(0);
      assertEquals(RECEIVING_PROCESS, receivingProductivities.getProcessName());
      assertEquals(168, receivingProductivities.getData().size());

      final var receivingProductivity = receivingProductivities.getData().get(23);
      assertEquals(FIRST_DATE_FOR_RECEIVING, receivingProductivity.getDayTime());
      assertEquals(278, receivingProductivity.getProductivity());

      final var checkInProductivities = productivities.get(1);
      assertEquals(CHECK_IN_PROCESS, checkInProductivities.getProcessName());
      assertEquals(168, checkInProductivities.getData().size());

      final var checkInProductivity = checkInProductivities.getData().get(0);
      assertEquals(FIRST_DATE, checkInProductivity.getDayTime());
      assertEquals(244, checkInProductivity.getProductivity());

      final var putAwayProductivities = productivities.get(2);
      assertEquals(PUT_AWAY_PROCESS, putAwayProductivities.getProcessName());
      assertEquals(168, putAwayProductivities.getData().size());

      final var putAwayProductivity = putAwayProductivities.getData().get(0);
      assertEquals(FIRST_DATE, putAwayProductivity.getDayTime());
      assertEquals(313, putAwayProductivity.getProductivity());

    } else {

      assertEquals(FIRST_VERSION, version);

      assertEquals(18, processingDistributions.size());

      assertEquals(2, productivities.size());

      final var checkInProductivities = productivities.get(0);
      assertEquals(CHECK_IN_PROCESS, checkInProductivities.getProcessName());
      assertEquals(168, checkInProductivities.getData().size());

      final var checkInProductivity = checkInProductivities.getData().get(0);
      assertEquals(FIRST_DATE, checkInProductivity.getDayTime());
      assertEquals(244, checkInProductivity.getProductivity());


      final var putAwayProductivities = productivities.get(1);
      assertEquals(PUT_AWAY_PROCESS, putAwayProductivities.getProcessName());
      assertEquals(168, putAwayProductivities.getData().size());

      final var putAwayProductivity = putAwayProductivities.getData().get(0);
      assertEquals(FIRST_DATE, putAwayProductivity.getDayTime());
      assertEquals(313, putAwayProductivity.getProductivity());

    }

    // POLYVALENCES
    assertTrue(result.getValues().containsKey(INBOUND_CHECKIN_PRODUCTIVITY_POLYVALENCES));
    assertTrue(result.getValues().containsKey(INBOUND_PUTAWAY_PRODUCTIVITY_POLIVALENCES));
    assertTrue(result.getValues().containsKey(INBOUND_RECEIVING_PRODUCTIVITY_POLYVALENCES));
  }
}
