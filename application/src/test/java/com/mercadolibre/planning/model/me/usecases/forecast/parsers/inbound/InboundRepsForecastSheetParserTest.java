package com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound;

import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.HEADCOUNT_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.INBOUND_CHECKIN_PRODUCTIVITY_POLYVALENCES;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.INBOUND_PUTAWAY_PRODUCTIVITY_POLIVALENCES;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.INBOUND_RECEIVING_PRODUCTIVITY_POLYVALENCES;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.PROCESSING_DISTRIBUTION;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InboundRepsForecastSheetParserTest {

  private static final String VALID_FILE_PATH = "inbound_planning_ok.xlsx";

  private static final String ERRONEOUS_FILE_PATH = "inbound_planning_with_errors.xlsx";

  private static final LogisticCenterConfiguration CONF =
      new LogisticCenterConfiguration(TimeZone.getTimeZone("UTC"));

  private static final ZonedDateTime FIRST_DATE = ZonedDateTime.parse("2021-11-07T00:00Z", ISO_OFFSET_DATE_TIME);

  private final InboundRepsForecastSheetParser parser = new InboundRepsForecastSheetParser();

  @Test
  @DisplayName("Excel Parsed Ok")
  void parseOk() {
    // GIVEN
    final MeliSheet repsSheet = getMeliSheetFrom(parser.name(), VALID_FILE_PATH);

    // WHEN
    final ForecastSheetDto result = parser.parse("ARBA01", repsSheet, CONF);

    // THEN
    assertNotNull(result);
    assertEquals("Plan de staffing", result.getSheetName());
    assertEquals(7, result.getValues().size());

    // PROCESSING DISTRIBUTIONS
    final var processingDistributions = (List<ProcessingDistribution>) result.getValues()
        .get(PROCESSING_DISTRIBUTION);

    assertNotNull(processingDistributions);
    assertEquals(17, processingDistributions.size());

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
    assertEquals(2, productivities.size());

    final var checkInProductivities = productivities.get(0);
    assertEquals("check_in", checkInProductivities.getProcessName());
    assertEquals(168, checkInProductivities.getData().size());

    final var checkInProductivity = checkInProductivities.getData().get(0);
    assertEquals(FIRST_DATE, checkInProductivity.getDayTime());
    assertEquals(407, checkInProductivity.getProductivity());

    final var putAwayProductivities = productivities.get(1);
    assertEquals("put_away", putAwayProductivities.getProcessName());
    assertEquals(168, putAwayProductivities.getData().size());

    final var putAwayProductivity = putAwayProductivities.getData().get(0);
    assertEquals(FIRST_DATE, putAwayProductivity.getDayTime());
    assertEquals(313, putAwayProductivity.getProductivity());

    // POLYVALENCES
    assertTrue(result.getValues().containsKey(INBOUND_CHECKIN_PRODUCTIVITY_POLYVALENCES));
    assertTrue(result.getValues().containsKey(INBOUND_PUTAWAY_PRODUCTIVITY_POLIVALENCES));
    assertTrue(result.getValues().containsKey(INBOUND_RECEIVING_PRODUCTIVITY_POLYVALENCES));
  }

  @Test
  @DisplayName("Excel With Errors in cells")
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
        "Error while trying to parse cell (D15) for sheet: Plan de staffing.\n"
            + "Error while trying to parse cell (B27) for sheet: Plan de staffing.\n"
            + "Error while trying to parse cell (J32) for sheet: Plan de staffing";

    assertEquals(expectedMessage, exception.getMessage());
    assertTrue(exceptionWarehouse.getMessage().contains("Warehouse id ARTW01 is different from warehouse id ARBA01 from file.\n"));
  }
}
