package com.mercadolibre.planning.model.me.controller.tools;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SaveUnitsResponse;
import com.mercadolibre.planning.model.me.usecases.sharedistribution.SaveShareDistribution;
import com.mercadolibre.planning.model.me.utils.DateUtils;
import com.newrelic.api.agent.Trace;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/middleend/share_distribution")
public class ShareDistributionController {


  SaveShareDistribution saveShareDistribution;

  @Trace
  @PostMapping("/execute")
  public ResponseEntity<List<SaveUnitsResponse>> run(@RequestParam @NotNull final List<String> warehouseIds,
                                                     @RequestParam @NotNull final int days,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME)
                                                     final ZonedDateTime initial
  ) {

    final ZonedDateTime dateFrom = initial == null ? DateUtils.getCurrentUtcDate().truncatedTo(ChronoUnit.DAYS).plusDays(1) : initial;


    return ResponseEntity.ok(saveShareDistribution.execute(warehouseIds, dateFrom, dateFrom.plusDays(days)));
  }

}
