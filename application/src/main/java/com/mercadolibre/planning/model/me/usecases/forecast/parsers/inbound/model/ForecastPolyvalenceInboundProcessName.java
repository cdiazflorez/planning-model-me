package com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model;

import java.util.Locale;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ForecastPolyvalenceInboundProcessName {
  RECEIVING(2),
  CHECKIN(3),
  PUTAWAY(4);

  private final int columnIndex;

  public String getName() {
    return name().toLowerCase(Locale.ENGLISH);
  }

  public static Stream<ForecastPolyvalenceInboundProcessName> stream() {
    return Stream.of(ForecastPolyvalenceInboundProcessName.values());
  }
}
