package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model;

import com.mercadolibre.planning.model.me.enums.ProcessPath;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.Value;

@Value
public class HeadcountProductivityRatio {
  ProcessPath processPath;
  List<HeadcountProductivityRatioData> data;

  @Value
  public static class HeadcountProductivityRatioData {
    ZonedDateTime date;
    double ratio;
  }
}
