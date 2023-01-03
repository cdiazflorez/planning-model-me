package com.mercadolibre.planning.model.me.entities.projection.monitoring;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import lombok.Value;

@Value
public class EndDayDeferralCard {

  Integer totalUnits;
  ZonedDateTime dateFrom;
  ZonedDateTime dateTo;

  public EndDayDeferralCard(Integer endDayDeferralTotal, ZonedDateTime dateFrom) {
    this.totalUnits = endDayDeferralTotal;
    this.dateFrom = dateFrom;
    this.dateTo = dateFrom.with(LocalTime.MAX);
  }

  public EndDayDeferralCard() {
    this.totalUnits = null;
    this.dateFrom = null;
    this.dateTo = null;
  }
}
