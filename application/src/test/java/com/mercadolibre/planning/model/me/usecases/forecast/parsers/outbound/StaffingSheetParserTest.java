package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound;

import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.HEADCOUNT_PRODUCTIVITY_PP;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.HEADCOUNT_RATIO;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.getMeliSheetFrom;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mercadolibre.planning.model.me.enums.ProcessPath;
import com.mercadolibre.planning.model.me.exception.ForecastParsingException;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.HeadcountProductivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.HeadcountProductivityData;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastSheetDto;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.HeadcountRatio;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StaffingSheetParserTest {
  private static final String VALID_FILE_PATH = "MVP1-outbound-test.xlsx";

  private static final String VALID_FILE_PATH2 = "Planning tools & Cap5 _ BRBA01 MVP 1  W13_23.xlsx";

  private static final String VALID_FILE_PATH_FOR_ARBA = "ARBA-outbound-test.xlsx";

  private static final String VALID_FILE_PATH_DISORDER_FOR_ARBA = "ARBA-outbound-disorder-test.xlsx";

  private static final String AR_WAREHOUSE_ID = "ARBA01";

  private static final String ERRONEOUS_FILE_PATH = "MVP1-outbound-ratio-error.xlsx";

  private static final String SHEET = "PP - Staffing";

  private static final LogisticCenterConfiguration CONF =
      new LogisticCenterConfiguration(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"));

  private static final Map<ProcessPath, Integer> INIT_PRODUCTIVITY_BY_PROCESS = Map.of(
      ProcessPath.TOT_MONO, 20,
      ProcessPath.TOT_MULTI_BATCH, 30,
      ProcessPath.TOT_MULTI_ORDER, 40,
      ProcessPath.NON_TOT_MONO, 50,
      ProcessPath.NON_TOT_MULTI_ORDER, 60,
      ProcessPath.NON_TOT_MULTI_BATCH, 70,
      ProcessPath.PP_DEFAULT_MONO, 80,
      ProcessPath.PP_DEFAULT_MULTI, 90
  );

  private static final Map<ProcessPath, Double> INIT_RATIO_BY_PROCESS = Map.of(
      ProcessPath.TOT_MONO, 0.80,
      ProcessPath.TOT_MULTI_BATCH, 0.02,
      ProcessPath.TOT_MULTI_ORDER, 0.03,
      ProcessPath.NON_TOT_MONO, 0.04,
      ProcessPath.NON_TOT_MULTI_ORDER, 0.05,
      ProcessPath.NON_TOT_MULTI_BATCH, 0.06
  );

  private static final Map<ProcessPath, Double> RATIO_BY_PROCESS = Map.of(
      ProcessPath.TOT_MONO, 0.02,
      ProcessPath.TOT_MULTI_BATCH, 0.03,
      ProcessPath.TOT_MULTI_ORDER, 0.04,
      ProcessPath.NON_TOT_MONO, 0.05,
      ProcessPath.NON_TOT_MULTI_ORDER, 0.06,
      ProcessPath.NON_TOT_MULTI_BATCH, 0.80
  );

  private static final Map<ProcessPath, Double> RATIO_BY_PROCESS_ARBA01 = Map.of(
      ProcessPath.TOT_MONO, 0.2,
      ProcessPath.TOT_MULTI_BATCH, 0.2,
      ProcessPath.TOT_MULTI_ORDER, 0.2,
      ProcessPath.NON_TOT_MONO, 0.2,
      ProcessPath.NON_TOT_MULTI_ORDER, 0.2
  );

  private final StaffingSheetParser staffingSheetParser = new StaffingSheetParser();

  @Test
  @DisplayName("Excel Parsed Ok")
  void parseOk() {
    // GIVEN
    // a valid sheet
    final var sheet = getMeliSheetFrom(staffingSheetParser.name(), VALID_FILE_PATH);

    // WHEN
    final ForecastSheetDto forecastSheetDto = staffingSheetParser.parse(WAREHOUSE_ID, sheet, CONF);

    // THEN
    assertNotNull(forecastSheetDto);
    assertValues(forecastSheetDto);
  }

  @Test
  @DisplayName("Excel Parsed Ok excel prod of brba01")
  void parseOk2() {
    // GIVEN
    // a valid sheet
    final var sheet = getMeliSheetFrom(staffingSheetParser.name(), VALID_FILE_PATH2);

    // WHEN
    final ForecastSheetDto forecastSheetDto = staffingSheetParser.parse("BRBA01", sheet, CONF);

    // THEN
    assertNotNull(forecastSheetDto);
  }

  @Test
  @DisplayName("Excel Parsed Ok")
  void arba01ParseOk() {
    // GIVEN
    // a valid sheet
    final var sheet = getMeliSheetFrom(staffingSheetParser.name(), VALID_FILE_PATH_FOR_ARBA);

    // WHEN
    final ForecastSheetDto forecastSheetDto = staffingSheetParser.parse(AR_WAREHOUSE_ID, sheet, CONF);

    // THEN
    assertNotNull(forecastSheetDto);
    assertValuesArba01(forecastSheetDto);
  }

  @Test
  @DisplayName("Excel with pp in disorder Parsed Ok ")
  void arba01ParseOkDisorder() {
    // GIVEN
    // a valid sheet
    final var sheet = getMeliSheetFrom(staffingSheetParser.name(), VALID_FILE_PATH_DISORDER_FOR_ARBA);

    // WHEN
    final ForecastSheetDto forecastSheetDto = staffingSheetParser.parse(AR_WAREHOUSE_ID, sheet, CONF);

    // THEN
    assertNotNull(forecastSheetDto);
    assertValuesArba01(forecastSheetDto);
  }

  @Test
  @DisplayName("Test when Melisheet is null then a ForecastParsingException is thrown")
  void errorURL() {
    // GIVEN

    // WHEN
    final ForecastParsingException exception = assertThrows(ForecastParsingException.class,
        () -> staffingSheetParser.parse("ARBA01", null, CONF));
    // THEN
    assertNotNull(exception.getMessage());
  }

  @Test
  @DisplayName("Excel With Errors")
  void errorsRatio() {
    // GIVEN
    final var sheet = getMeliSheetFrom(staffingSheetParser.name(), ERRONEOUS_FILE_PATH);

    // WHEN
    final ForecastParsingException exception = assertThrows(ForecastParsingException.class,
        () -> staffingSheetParser.parse("ARTW01", sheet, CONF));

    // THEN
    assertNotNull(exception.getMessage());
    final String expectedMessage = "Ratio out range = 2021-01-24T00:00:00 for date = 1.04, expected ratio from 0.99 to 1.01";

    assertEquals(expectedMessage, exception.getMessage());
  }

  private void assertValues(final ForecastSheetDto forecastSheetDto) {
    assertTrue(SHEET.equalsIgnoreCase(forecastSheetDto.getSheetName()));
    assertEquals(2, forecastSheetDto.getValues().values().size());

    final var values = forecastSheetDto.getValues();
    assertTrue(values.containsKey(HEADCOUNT_PRODUCTIVITY_PP));
    assertHeadcountProductivity((List<HeadcountProductivity>) values.get(HEADCOUNT_PRODUCTIVITY_PP));

    assertTrue(values.containsKey(HEADCOUNT_RATIO));
    assertHeadcountRatio((List<HeadcountRatio>) values.get(HEADCOUNT_RATIO));
  }

  private void assertValuesArba01(final ForecastSheetDto forecastSheetDto) {
    assertTrue(SHEET.equalsIgnoreCase(forecastSheetDto.getSheetName()));
    assertEquals(2, forecastSheetDto.getValues().values().size());

    final var values = forecastSheetDto.getValues();
    assertTrue(values.containsKey(HEADCOUNT_PRODUCTIVITY_PP));
    assertHeadcountProductivityArba01((List<HeadcountProductivity>) values.get(HEADCOUNT_PRODUCTIVITY_PP));

    assertTrue(values.containsKey(HEADCOUNT_RATIO));
    assertHeadcountRatioARBA01((List<HeadcountRatio>) values.get(HEADCOUNT_RATIO));
  }

  private void assertHeadcountProductivity(List<HeadcountProductivity> hcProductivity) {
    assertEquals(6, hcProductivity.size());

    hcProductivity.forEach(hc -> {
          hc.getData().sort(Comparator.comparing(HeadcountProductivityData::getProductivity));
          IntStream.range(0, hc.getData().size())
              .forEach(i -> {
                long expectedProductivity = getProductivityByProcessAndIndex(hc.getProcessPath(), i);
                assertEquals(expectedProductivity, hc.getData().get(i).getProductivity());
              });
        }
    );
  }

  private void assertHeadcountProductivityArba01(List<HeadcountProductivity> hcProductivity) {
    assertEquals(5, hcProductivity.size());

    hcProductivity.forEach(hc -> {
          hc.getData().sort(Comparator.comparing(HeadcountProductivityData::getProductivity));
          IntStream.range(0, hc.getData().size())
              .forEach(i -> {
                long expectedProductivity = getProductivityByProcessAndIndex(hc.getProcessPath(), i);
                assertEquals(expectedProductivity, hc.getData().get(i).getProductivity());
              });
        }
    );
  }

  private long getProductivityByProcessAndIndex(final ProcessPath processPath, final int index) {
    return INIT_PRODUCTIVITY_BY_PROCESS.get(processPath) + index;
  }

  private void assertHeadcountRatioARBA01(final List<HeadcountRatio> hcRatio) {
    assertEquals(5, hcRatio.size());

    hcRatio.forEach(hc -> IntStream.range(0, hc.getData().size())
        .forEach(i -> {
          Double expectedRatio = getRatioByProcessAndIndexForArbaTest(hc.getProcessPath().getName());
          assertEquals(expectedRatio, hc.getData().get(i).getRatio());
        })
    );
  }

  private void assertHeadcountRatio(final List<HeadcountRatio> hcRatio) {
    assertEquals(6, hcRatio.size());

    AtomicInteger index = new AtomicInteger();

    hcRatio.forEach(hc -> IntStream.range(0, hc.getData().size())
        .forEach(i -> {
          Double expectedRatio = getRatioByProcessAndIndex(hc.getProcessPath().getName(), i);
          assertEquals(expectedRatio, hc.getData().get(i).getRatio());
          index.getAndIncrement();
        })
    );
  }


  private Double getRatioByProcessAndIndex(final String processPath, final int index) {
    final ProcessPath pp = ProcessPath.from(processPath);
    if (index == 0) {
      return INIT_RATIO_BY_PROCESS.get(pp);
    } else {
      return RATIO_BY_PROCESS.get(pp);
    }
  }

  private Double getRatioByProcessAndIndexForArbaTest(final String processPath) {
    final ProcessPath pp = ProcessPath.from(processPath);
    return RATIO_BY_PROCESS_ARBA01.get(pp);
  }
}
