package com.mercadolibre.planning.model.me.usecases.forecast;

import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.enums.ProcessPath.GLOBAL;
import static com.mercadolibre.planning.model.me.enums.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.me.enums.ProcessPath.NON_TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.me.enums.ProcessPath.NON_TOT_MULTI_ORDER;
import static com.mercadolibre.planning.model.me.enums.ProcessPath.PP_DEFAULT_MONO;
import static com.mercadolibre.planning.model.me.enums.ProcessPath.PP_DEFAULT_MULTI;
import static com.mercadolibre.planning.model.me.enums.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.me.enums.ProcessPath.TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.me.enums.ProcessPath.TOT_MULTI_ORDER;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.HEADCOUNT_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.HEADCOUNT_PRODUCTIVITY_PP;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.HEADCOUNT_RATIO;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.PROCESSING_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.ACTIVE_WORKERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mercadolibre.planning.model.me.exception.ForecastParsingException;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.HeadcountProductivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.HeadcountProductivityData;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingDistribution;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingDistributionData;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastColumn;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.HeadcountRatio;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MappingOutboundTest {

  private static final int DEFAULT_ABILITY_LEVEL = 1;

  private static final ZonedDateTime DATE = ZonedDateTime.ofInstant(Instant.parse("2022-10-20T00:00:00Z"),
      ZoneId.of("America/Argentina/Buenos_Aires"));

  @Test
  void buildHeadcountProductivityTest() {
    List<HeadcountProductivity> headcountProductivities = MappingOutbound.buildHeadcountProductivity(parsedValuesProductivity());
    assertEquals(expectedHeadcountProductivity().size(), headcountProductivities.size());

    var expectedSorted =
        expectedHeadcountProductivity().stream()
            .sorted(Comparator.comparing(HeadcountProductivity::getProcessName))
            .sorted(Comparator.comparing(HeadcountProductivity::getProcessPath))
            .collect(Collectors.toList());

    var actualSorted = headcountProductivities.stream()
        .sorted(Comparator.comparing(HeadcountProductivity::getProcessName))
        .sorted(Comparator.comparing(HeadcountProductivity::getProcessPath))
        .collect(Collectors.toList());

    assertEquals(expectedSorted, actualSorted);
  }

  @Test
  void buildProcessingDistributionTest() {
    List<ProcessingDistribution> processingDistributions = MappingOutbound.buildProcessingDistribution(parsedValuesRatio());
    assertEquals(expectedProcessingDistribution().size(), processingDistributions.size());

    var expectedSorted = expectedProcessingDistribution().stream()
        .sorted(Comparator.comparing(ProcessingDistribution::getProcessName))
        .sorted(Comparator.comparing(ProcessingDistribution::getProcessPath))
        .collect(Collectors.toList());

    var actualSorted = processingDistributions.stream()
        .sorted(Comparator.comparing(ProcessingDistribution::getProcessName))
        .sorted(Comparator.comparing(ProcessingDistribution::getProcessPath))
        .collect(Collectors.toList());

    assertEquals(expectedSorted, actualSorted);
  }

  @Test
  @DisplayName("given non overlapping dates for headcount and ratios then an exception should be thrown")
  void buildProcessingDistributionTestWithNonOverlappingDates() {
    // GIVEN
    final var headcount = processingDistributions();
    final var ratios = List.of(
        new HeadcountRatio(
            TOT_MONO,
            List.of(new HeadcountRatio.HeadcountRatioData(DATE.plusHours(1L), 0.01))
        )
    );

    final Map<ForecastColumn, Object> input = Map.of(
        PROCESSING_DISTRIBUTION, headcount,
        HEADCOUNT_RATIO, ratios
    );

    // WHEN
    final var e = assertThrows(
        ForecastParsingException.class,
        () -> MappingOutbound.buildProcessingDistribution(input)
    );

    assertTrue(e.getMessage().contains("dates"));
  }

  @Test
  @DisplayName("given more dates for ratios than headcount then an exception should be thrown")
  void buildProcessingDistributionTestWithDisjointDatesSet() {
    // GIVEN
    final var headcount = processingDistributions();
    final var ratios = List.of(
        new HeadcountRatio(
            TOT_MONO,
            List.of(
                new HeadcountRatio.HeadcountRatioData(DATE.plusHours(1L), 0.01),
                new HeadcountRatio.HeadcountRatioData(DATE.plusHours(2L), 0.01)
            )
        )
    );

    final Map<ForecastColumn, Object> input = Map.of(
        PROCESSING_DISTRIBUTION, headcount,
        HEADCOUNT_RATIO, ratios
    );

    // WHEN
    final var e = assertThrows(
        ForecastParsingException.class,
        () -> MappingOutbound.buildProcessingDistribution(input)
    );

    assertTrue(e.getMessage().contains("dates"));
  }

  private Map<ForecastColumn, Object> parsedValuesProductivity() {
    return Map.of(
        HEADCOUNT_PRODUCTIVITY_PP, headcountProductivityByPP(),
        HEADCOUNT_PRODUCTIVITY, headcountProductivities()
    );
  }

  private Map<ForecastColumn, Object> parsedValuesRatio() {
    return Map.of(
        PROCESSING_DISTRIBUTION, processingDistributions(),
        HEADCOUNT_RATIO, headcountRatio()
    );
  }

  private List<HeadcountProductivity> expectedHeadcountProductivity() {
    return List.of(
        new HeadcountProductivity(
            GLOBAL,
            PICKING.getName(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            DEFAULT_ABILITY_LEVEL,
            List.of(
                new HeadcountProductivityData(
                    DATE,
                    3600
                )
            )
        ),
        new HeadcountProductivity(
            GLOBAL,
            PACKING.getName(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            DEFAULT_ABILITY_LEVEL,
            List.of(
                new HeadcountProductivityData(
                    DATE,
                    1000
                )
            )
        ),

        new HeadcountProductivity(
            GLOBAL,
            WAVING.getName(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            DEFAULT_ABILITY_LEVEL,
            List.of(
                new HeadcountProductivityData(
                    DATE,
                    2000
                )
            )
        ),
        new HeadcountProductivity(
            TOT_MONO,
            PICKING.getName(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            DEFAULT_ABILITY_LEVEL,
            List.of(
                new HeadcountProductivityData(
                    DATE,
                    100
                )
            )
        ),

        new HeadcountProductivity(
            TOT_MULTI_BATCH,
            PICKING.getName(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            DEFAULT_ABILITY_LEVEL,
            List.of(
                new HeadcountProductivityData(
                    DATE,
                    200
                )
            )
        ),

        new HeadcountProductivity(
            TOT_MULTI_ORDER,
            PICKING.getName(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            DEFAULT_ABILITY_LEVEL,
            List.of(
                new HeadcountProductivityData(
                    DATE,
                    300
                )
            )
        ),
        new HeadcountProductivity(
            NON_TOT_MONO,
            PICKING.getName(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            DEFAULT_ABILITY_LEVEL,
            List.of(
                new HeadcountProductivityData(
                    DATE,
                    400
                )
            )
        ),
        new HeadcountProductivity(
            NON_TOT_MULTI_BATCH,
            PICKING.getName(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            DEFAULT_ABILITY_LEVEL,
            List.of(
                new HeadcountProductivityData(
                    DATE,
                    500
                )
            )
        ),
        new HeadcountProductivity(
            NON_TOT_MULTI_ORDER,
            PICKING.getName(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            DEFAULT_ABILITY_LEVEL,
            List.of(
                new HeadcountProductivityData(
                    DATE,
                    600
                )
            )
        ),
        new HeadcountProductivity(
            PP_DEFAULT_MONO,
            PICKING.getName(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            DEFAULT_ABILITY_LEVEL,
            List.of(
                new HeadcountProductivityData(
                    DATE,
                    700
                )
            )
        ),
        new HeadcountProductivity(
            PP_DEFAULT_MULTI,
            PICKING.getName(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            DEFAULT_ABILITY_LEVEL,
            List.of(
                new HeadcountProductivityData(
                    DATE,
                    800
                )
            )
        )
    );
  }

  private List<HeadcountProductivity> headcountProductivityByPP() {
    return List.of(
        new HeadcountProductivity(
            TOT_MONO,
            PICKING.getName(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            DEFAULT_ABILITY_LEVEL,
            List.of(
                new HeadcountProductivityData(
                    DATE,
                    100
                )
            )
        ),

        new HeadcountProductivity(
            TOT_MULTI_BATCH,
            PICKING.getName(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            DEFAULT_ABILITY_LEVEL,
            List.of(
                new HeadcountProductivityData(
                    DATE,
                    200
                )
            )
        ),

        new HeadcountProductivity(
            TOT_MULTI_ORDER,
            PICKING.getName(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            DEFAULT_ABILITY_LEVEL,
            List.of(
                new HeadcountProductivityData(
                    DATE,
                    300
                )
            )
        ),
        new HeadcountProductivity(
            NON_TOT_MONO,
            PICKING.getName(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            DEFAULT_ABILITY_LEVEL,
            List.of(
                new HeadcountProductivityData(
                    DATE,
                    400
                )
            )
        ),
        new HeadcountProductivity(
            NON_TOT_MULTI_BATCH,
            PICKING.getName(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            DEFAULT_ABILITY_LEVEL,
            List.of(
                new HeadcountProductivityData(
                    DATE,
                    500
                )
            )
        ),
        new HeadcountProductivity(
            NON_TOT_MULTI_ORDER,
            PICKING.getName(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            DEFAULT_ABILITY_LEVEL,
            List.of(
                new HeadcountProductivityData(
                    DATE,
                    600
                )
            )
        ),
        new HeadcountProductivity(
            PP_DEFAULT_MONO,
            PICKING.getName(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            DEFAULT_ABILITY_LEVEL,
            List.of(
                new HeadcountProductivityData(
                    DATE,
                    700
                )
            )
        ),
        new HeadcountProductivity(
            PP_DEFAULT_MULTI,
            PICKING.getName(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            DEFAULT_ABILITY_LEVEL,
            List.of(
                new HeadcountProductivityData(
                    DATE,
                    800
                )
            )
        )
    );
  }

  private List<HeadcountProductivity> headcountProductivities() {
    return List.of(
        new HeadcountProductivity(
            GLOBAL,
            PICKING.getName(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            DEFAULT_ABILITY_LEVEL,
            List.of(
                new HeadcountProductivityData(
                    DATE,
                    3600
                )
            )
        ),
        new HeadcountProductivity(
            GLOBAL,
            PACKING.getName(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            DEFAULT_ABILITY_LEVEL,
            List.of(
                new HeadcountProductivityData(
                    DATE,
                    1000
                )
            )
        ),

        new HeadcountProductivity(
            GLOBAL,
            WAVING.getName(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            DEFAULT_ABILITY_LEVEL,
            List.of(
                new HeadcountProductivityData(
                    DATE,
                    2000
                )
            )
        )
    );
  }

  private List<HeadcountRatio> headcountRatio() {
    return List.of(
        new HeadcountRatio(
            TOT_MONO,
            List.of(new HeadcountRatio.HeadcountRatioData(DATE, 0.01))
        ),
        new HeadcountRatio(
            TOT_MULTI_BATCH,
            List.of(new HeadcountRatio.HeadcountRatioData(DATE, 0.02))
        ),
        new HeadcountRatio(
            TOT_MULTI_ORDER,
            List.of(new HeadcountRatio.HeadcountRatioData(DATE, 0.03))
        ),
        new HeadcountRatio(
            NON_TOT_MONO,
            List.of(new HeadcountRatio.HeadcountRatioData(DATE, 0.04))
        ),
        new HeadcountRatio(
            NON_TOT_MULTI_ORDER,
            List.of(new HeadcountRatio.HeadcountRatioData(DATE, 0.05))
        ),
        new HeadcountRatio(
            NON_TOT_MULTI_BATCH,
            List.of(new HeadcountRatio.HeadcountRatioData(DATE, 0.06))
        ),
        new HeadcountRatio(
            PP_DEFAULT_MONO,
            List.of(new HeadcountRatio.HeadcountRatioData(DATE, 0.07))
        ),
        new HeadcountRatio(
            PP_DEFAULT_MULTI,
            List.of(new HeadcountRatio.HeadcountRatioData(DATE, 0.72))
        )
    );
  }

  private List<ProcessingDistribution> expectedProcessingDistribution() {
    return List.of(
        new ProcessingDistribution(ACTIVE_WORKERS.toString(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            PICKING.getName(),
            GLOBAL,
            List.of(
                new ProcessingDistributionData(
                    DATE,
                    100
                )
            )
        ),
        new ProcessingDistribution(ACTIVE_WORKERS.toString(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            PACKING.getName(),
            GLOBAL,
            List.of(
                new ProcessingDistributionData(
                    DATE,
                    100
                )
            )
        ),
        new ProcessingDistribution(ACTIVE_WORKERS.toString(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            PICKING.getName(),
            TOT_MONO,
            List.of(
                new ProcessingDistributionData(
                    DATE,
                    1
                )
            )
        ),
        new ProcessingDistribution(ACTIVE_WORKERS.toString(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            PICKING.getName(),
            TOT_MULTI_BATCH,
            List.of(
                new ProcessingDistributionData(
                    DATE,
                    2
                )
            )
        ),
        new ProcessingDistribution(ACTIVE_WORKERS.toString(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            PICKING.getName(),
            TOT_MULTI_ORDER,
            List.of(
                new ProcessingDistributionData(
                    DATE,
                    3
                )
            )
        ),
        new ProcessingDistribution(ACTIVE_WORKERS.toString(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            PICKING.getName(),
            NON_TOT_MONO,
            List.of(
                new ProcessingDistributionData(
                    DATE,
                    4
                )
            )
        ),
        new ProcessingDistribution(ACTIVE_WORKERS.toString(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            PICKING.getName(),
            NON_TOT_MULTI_ORDER,
            List.of(
                new ProcessingDistributionData(
                    DATE,
                    5
                )
            )
        ),
        new ProcessingDistribution(ACTIVE_WORKERS.toString(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            PICKING.getName(),
            NON_TOT_MULTI_BATCH,
            List.of(
                new ProcessingDistributionData(
                    DATE,
                    6
                )
            )
        ),
        new ProcessingDistribution(ACTIVE_WORKERS.toString(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            PICKING.getName(),
            PP_DEFAULT_MONO,
            List.of(
                new ProcessingDistributionData(
                    DATE,
                    7
                )
            )
        ),
        new ProcessingDistribution(ACTIVE_WORKERS.toString(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            PICKING.getName(),
            PP_DEFAULT_MULTI,
            List.of(
                new ProcessingDistributionData(
                    DATE,
                    72
                )
            )
        )
    );
  }

  private List<ProcessingDistribution> processingDistributions() {
    return List.of(
        new ProcessingDistribution(ACTIVE_WORKERS.toString(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            PICKING.getName(),
            GLOBAL,
            List.of(
                new ProcessingDistributionData(
                    DATE,
                    100
                )
            )
        ),
        new ProcessingDistribution(ACTIVE_WORKERS.toString(),
            MetricUnit.UNITS_PER_HOUR.getName(),
            PACKING.getName(),
            GLOBAL,
            List.of(
                new ProcessingDistributionData(
                    DATE,
                    100
                )
            )
        )
    );
  }
}

