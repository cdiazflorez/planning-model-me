package com.mercadolibre.planning.model.me.usecases.forecast.utils;

import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.NON_SYSTEMIC_COLUMN_COUNT;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.NON_SYSTEMIC_COLUMN_NAME;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.PROCESSING_DISTRIBUTION_COLUMN_NAME_ROW;

import com.mercadolibre.spreadsheet.MeliCell;
import com.mercadolibre.spreadsheet.MeliSheet;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@AllArgsConstructor
public enum SheetVersion {
  INITIAL_VERSION("1.0", validateInitialVersion()),
  NON_SYSTEMIC_VERSION_OB("2.0", validateNonSystemicVersion());

  private String version;

  private Predicate<MeliSheet> validateVersion;

  public static SheetVersion getSheetVersion(final MeliSheet meliSheet) {
    return Arrays.stream(values())
        .filter(version1 -> version1.getValidateVersion().test(meliSheet))
        .findFirst()
        .orElse(SheetVersion.INITIAL_VERSION);
  }

  public static <T> Map<SheetVersion, T> mapping(final T initial, final T nonSystemic) {
    return Map.of(INITIAL_VERSION, initial, NON_SYSTEMIC_VERSION_OB, nonSystemic);
  }

  private static Predicate<MeliSheet> validateInitialVersion() {
    return (MeliSheet a) -> !validateNonSystemicVersion().test(a);
  }

  private static Predicate<MeliSheet> validateNonSystemicVersion() {
    return (MeliSheet meliSheet) -> {
      final long nonSystemicCount =
          meliSheet.getRowAt(PROCESSING_DISTRIBUTION_COLUMN_NAME_ROW).getCells().stream()
              .map(MeliCell::getValue)
              .filter(Objects::nonNull)
              .filter(s -> s.contains(NON_SYSTEMIC_COLUMN_NAME))
              .count();

      return nonSystemicCount == NON_SYSTEMIC_COLUMN_COUNT;
    };
  }
}
