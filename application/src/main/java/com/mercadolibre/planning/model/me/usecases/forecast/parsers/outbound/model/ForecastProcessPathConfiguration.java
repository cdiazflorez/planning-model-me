package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model;

import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getStringValueAt;

import com.mercadolibre.planning.model.me.enums.ProcessPath;
import com.mercadolibre.planning.model.me.usecases.forecast.utils.ProcessPathNotFoundException;
import com.mercadolibre.spreadsheet.MeliRow;
import com.mercadolibre.spreadsheet.MeliSheet;
import java.util.Arrays;
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

  private static final int COLUMN_COUNT = values().length;

  private final Integer offPP;

  private final Integer onPP;

  public static boolean hasProcessPath(final MeliSheet sheet, final MeliRow row) {
      final Integer ppColumnPosition = PROCESS_PATH_ID_COLUMN.getOnPP();

      if (ppColumnPosition == null || getStringValueAt(sheet, row, ppColumnPosition) == null) {
          return false;
      }

      final String ppColumnValue = getStringValueAt(sheet, row, ppColumnPosition).toLowerCase(Locale.getDefault());

      //TODO: Avoid knowing all process paths
      try {
          return Arrays.asList(ProcessPath.values()).contains(ProcessPath.from(ppColumnValue));
      } catch (ProcessPathNotFoundException processPathNotFoundException) {
          return false;
      }

  }

  public static int maxValidateColumnIndex(final MeliSheet sheet, final MeliRow row) {
    return hasProcessPath(sheet, row) ? COLUMN_COUNT : COLUMN_COUNT - 1;
  }

  public Integer getPosition(final MeliSheet sheet, final MeliRow row) {
      return hasProcessPath(sheet, row) ? this.onPP : this.offPP;
  }
}
