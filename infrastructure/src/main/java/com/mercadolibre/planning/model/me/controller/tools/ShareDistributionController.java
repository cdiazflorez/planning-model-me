package com.mercadolibre.planning.model.me.controller.tools;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SaveUnitsResponse;
import com.mercadolibre.planning.model.me.usecases.sharedistribution.SaveShareDistribution;
import com.mercadolibre.planning.model.me.utils.DateUtils;
import com.newrelic.api.agent.Trace;
import java.time.Instant;
import java.time.ZonedDateTime;
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


/**
 * Controller for Jobs.
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/middleend/share_distribution")
public class ShareDistributionController {


  SaveShareDistribution saveShareDistribution;

  @Trace
  @PostMapping("/execute")
  public ResponseEntity<List<SaveUnitsResponse>> run(@RequestParam @NotNull final List<String> warehouseIds,
                                                     @RequestParam(defaultValue = "6") @NotNull final int windowSize,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME)
                                                     final Instant viewDate
  ) {

    Instant now = Instant.now();
    return ResponseEntity.ok(saveShareDistribution.execute(warehouseIds, viewDate, windowSize, now));
  }

}
