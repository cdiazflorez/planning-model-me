package com.mercadolibre.planning.model.me.entities.projection;

import com.mercadolibre.planning.model.me.entities.projection.dateselector.DateSelector;
import java.time.ZonedDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PlanningView {

  Boolean isNewVersion;

  ZonedDateTime currentDate;
  /**
   * Projection/Deferrer date selector.
   */
  DateSelector dateSelector;

  /**
   * Message to show when the forecast is missing.
   */
  String emptyStateMessage;

  ResultData data;

}
