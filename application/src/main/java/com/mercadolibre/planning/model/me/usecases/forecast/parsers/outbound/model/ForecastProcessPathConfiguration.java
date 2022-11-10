package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model;

import java.util.List;
import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ForecastProcessPathConfiguration {
  PD_DATE_OUT_COLUMN(1, 1),
  PD_DATE_IN_COLUMN(2, 2),
  PROCESS_PATH_ID_COLUMN(null, 3),
  CARRIER_ID_COLUMN(3, 4),
  SERVICE_ID_COLUMN(4, 5),
  CANALIZATION_COLUMN(5, 6),
  PD_QUANTITY_COLUMN(6, 7);

  //TODO Change ARTW01 by BRBA01 when starting pilot.
  private static final List<String> LOGISTIC_CENTER_WITH_PP = List.of("ARTW01");

  private static final int COLUMN_COUNT = values().length;

  private final Integer offPP;

  private final Integer onPP;

  public static boolean hasProcessPath(final String logisticCenter) {
    return LOGISTIC_CENTER_WITH_PP.contains(logisticCenter.toUpperCase(Locale.getDefault()));
  }

  public static int maxValidateColumnIndex(final String logisticCenter) {
    return hasProcessPath(logisticCenter) ? COLUMN_COUNT : COLUMN_COUNT - 1;
  }

  public Integer getPosition(final String logisticCenter) {
    return hasProcessPath(logisticCenter) ? this.onPP : this.offPP;
  }
}
