package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import com.mercadolibre.planning.model.me.enums.ProcessPath;
import java.util.List;
import lombok.Value;

@Value
public class HeadcountProductivity {
  ProcessPath processPath;
  String processName;
  String productivityMetricUnit;
  int abilityLevel;
  List<HeadcountProductivityData> data;
}
