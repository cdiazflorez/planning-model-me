package com.mercadolibre.planning.model.me.usecases.forecast.utils;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
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
  INITIAL_VERSION(
          "1.0",
          Map.of(
                  FBM_WMS_OUTBOUND, validateOutboundInitialVersion(),
                  FBM_WMS_INBOUND, validateInboundInitialVersion()
          )
  ),
  SECOND_VERSION(
          "2.0",
          Map.of(
                  FBM_WMS_OUTBOUND, validateNonSystemicVersion(),
                  FBM_WMS_INBOUND, validateSystemicReceivingVersion()
          )
  );

  public static final int PROCESSING_DISTRIBUTION_COLUMN_NAME_ROW = 6;

  public static final int NON_SYSTEMIC_COLUMN_COUNT = 5;

  public static final String NON_SYSTEMIC_COLUMN_NAME = "No Sistemicos";

  public static final String SYSTEMIC_RECEIVING_COLUMN_NAME = "Receiving S";

  public static final int SYSTEMIC_RECEIVING_COLUMN_COUNT = 2;

  private final String version;

  private final Map<Workflow, Predicate<MeliSheet>> validateVersionByWorkflow;

  public static SheetVersion getSheetVersion(final MeliSheet meliSheet, final Workflow workflow) {
    return Arrays.stream(values())
        .filter(version1 -> version1.getValidateVersionByWorkflow().get(workflow).test(meliSheet))
        .findFirst()
        .orElse(SheetVersion.INITIAL_VERSION);
  }

  public static <T> Map<SheetVersion, T> mapping(final T initial, final T secondVersion) {
    return Map.of(INITIAL_VERSION, initial, SECOND_VERSION, secondVersion);
  }

  private static Predicate<MeliSheet> validateOutboundInitialVersion() {
    return (MeliSheet a) -> !validateNonSystemicVersion().test(a);
  }

  private static Predicate<MeliSheet> validateNonSystemicVersion() {
    return (MeliSheet meliSheet) ->
        getNonSystemicColumnCount(meliSheet) == NON_SYSTEMIC_COLUMN_COUNT;
  }

  private static long getNonSystemicColumnCount(final MeliSheet meliSheet) {
    return meliSheet.getRowAt(PROCESSING_DISTRIBUTION_COLUMN_NAME_ROW).getCells().stream()
        .map(MeliCell::getValue)
        .filter(Objects::nonNull)
        .filter(s -> s.contains(NON_SYSTEMIC_COLUMN_NAME))
        .count();
  }

  private static Predicate<MeliSheet> validateInboundInitialVersion() {
      return (final MeliSheet meliSheet) -> !validateSystemicReceivingVersion().test(meliSheet);
  }

  private static Predicate<MeliSheet> validateSystemicReceivingVersion() {
      return (final MeliSheet meliSheet) -> {
          final long systemicReceiving = meliSheet.getRowAt(PROCESSING_DISTRIBUTION_COLUMN_NAME_ROW).getCells().stream()
                  .map(MeliCell::getValue)
                  .filter(Objects::nonNull)
                  .filter(v -> v.contains(SYSTEMIC_RECEIVING_COLUMN_NAME))
                  .count();
          return SYSTEMIC_RECEIVING_COLUMN_COUNT == systemicReceiving;
      };
  }

}
