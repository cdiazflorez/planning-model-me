package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound;

import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.HEADCOUNT_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.HEADCOUNT_RATIO_PRODUCTIVITY;
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
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.HeadcountProductivityRatio;
import com.mercadolibre.spreadsheet.MeliSheet;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StaffingSheetParserTest {
  private static final String VALID_FILE_PATH = "MVP1-outbound-test.xlsx";

  private static final String ERRONEOUS_FILE_PATH = "MVP1-outbound-ratio-error.xlsx";

  private static final String SHEET = "PP - Staffing";

  private static final String MIDDLE = "middle";

  private static final String FINAL = "final";

  private static final int FINAL_INDEX_ROW = 167;

  private static final LogisticCenterConfiguration CONF =
      new LogisticCenterConfiguration(TimeZone.getDefault());

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
      ProcessPath.TOT_MONO, 0.01,
      ProcessPath.TOT_MULTI_BATCH, 0.02,
      ProcessPath.TOT_MULTI_ORDER, 0.03,
      ProcessPath.NON_TOT_MONO, 0.04,
      ProcessPath.NON_TOT_MULTI_ORDER, 0.05,
      ProcessPath.NON_TOT_MULTI_BATCH, 0.06,
      ProcessPath.PP_DEFAULT_MONO, 0.07,
      ProcessPath.PP_DEFAULT_MULTI, 0.72
  );

  private static final Map<String, Double> RATIO_PP_DEFAULT_MULTI = Map.of(
      MIDDLE, 0.65,
      FINAL, 0.58
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
  @DisplayName("Test when Melisheet is null then a ForecastParsingException is thrown")
  void errorURL() {
    // GIVEN
    final MeliSheet staffingSheet = null;

    // WHEN
    final ForecastParsingException exception = assertThrows(ForecastParsingException.class,
        () -> staffingSheetParser.parse("ARBA01", staffingSheet, CONF));
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
        () -> staffingSheetParser.parse("ARBA01", sheet, CONF));

    // THEN
    assertNotNull(exception.getMessage());
    final String expectedMessage = "Ratio out range = 1.27, expected ratio from 0.99 to 1.01";

    assertEquals(expectedMessage, exception.getMessage());
  }

  private void assertValues(final ForecastSheetDto forecastSheetDto) {
    assertTrue(SHEET.equalsIgnoreCase(forecastSheetDto.getSheetName()));
    assertEquals(2, forecastSheetDto.getValues().values().size());

    final var values = forecastSheetDto.getValues();
    assertTrue(values.containsKey(HEADCOUNT_PRODUCTIVITY));
    assertHeadcountProductivity((List<HeadcountProductivity>) values.get(HEADCOUNT_PRODUCTIVITY));

    assertTrue(values.containsKey(HEADCOUNT_RATIO_PRODUCTIVITY));
    assertHeadcountRatio((List<HeadcountProductivityRatio>) values.get(HEADCOUNT_RATIO_PRODUCTIVITY));
  }

  private void assertHeadcountProductivity(List<HeadcountProductivity> hcProductivity) {
    assertEquals(8, hcProductivity.size());

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

  private void assertHeadcountRatio(final List<HeadcountProductivityRatio> hcRatio) {
    assertEquals(8, hcRatio.size());

    AtomicInteger index = new AtomicInteger();

    hcRatio.forEach(hc -> {
          IntStream.range(0, hc.getData().size())
              .forEach(i -> {
                Double expectedRatio = getRatioByProcessAndIndex(hc.getProcessPath().getName(), i);
                assertEquals(expectedRatio, hc.getData().get(i).getRatio());
                index.getAndIncrement();
              });
        }
    );
  }

  private Double getRatioByProcessAndIndex(final String processPath, final int index) {
    final ProcessPath pp = ProcessPath.from(processPath);
    final DecimalFormat doubleFormat = new DecimalFormat("#.00");
    if (index == 0) {
      return INIT_RATIO_BY_PROCESS.get(pp);
    } else if (index < FINAL_INDEX_ROW) {
      if (pp == ProcessPath.PP_DEFAULT_MULTI) {
        return RATIO_PP_DEFAULT_MULTI.get(MIDDLE);
      }
      return Double.valueOf(doubleFormat.format(INIT_RATIO_BY_PROCESS.get(pp) + 0.01));
    }

    if (pp == ProcessPath.PP_DEFAULT_MULTI) {
      return RATIO_PP_DEFAULT_MULTI.get(FINAL);
    } else {
      return Double.valueOf(doubleFormat.format(INIT_RATIO_BY_PROCESS.get(pp) + 0.02));
    }

  }
}
