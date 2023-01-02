package com.mercadolibre.planning.model.me.entities.projection.monitoring;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import lombok.Value;

@Value
public class EndDayDeferralCard {

  Integer endDayDeferralTotal;
  ZonedDateTime dateFrom;
  ZonedDateTime dateTo;

  public EndDayDeferralCard(Integer endDayDeferralTotal, ZonedDateTime dateFrom) {
    this.endDayDeferralTotal = endDayDeferralTotal;
    this.dateFrom = dateFrom;
    this.dateTo = dateFrom.with(LocalTime.MAX);
  }
}
