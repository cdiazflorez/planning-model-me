package com.mercadolibre.planning.model.me.entities.projection;

import java.time.Instant;
import lombok.Value;

@Value
public class Projection {
  Instant cpt;
  Instant projectedEndDate;
  long currentBacklog;
  Long forecastedUnits;
  long soldUnits;
  int cycleTime;
  int remainingQuantity;
  boolean isDeferred;
  boolean isExpired;
  Double forecastDeviation;
  Instant simulatedEndDate;
  Instant deferredAt;
  int deferredUnits;
  String deferralStatus;
}
