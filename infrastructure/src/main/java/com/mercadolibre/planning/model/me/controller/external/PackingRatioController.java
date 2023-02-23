package com.mercadolibre.planning.model.me.controller.external;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PackingRatio;
import com.mercadolibre.planning.model.me.services.backlog.RatioService;
import java.time.Instant;
import java.util.Map;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/middleend")
public class PackingRatioController {

  private RatioService ratioService;

  @GetMapping("/packing_ratio")
  public ResponseEntity<Map<Instant, PackingRatio>> getPackingRatio(
      @RequestParam @NotNull @NotBlank final String logisticCenterId,
      @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Instant dateFrom,
      @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Instant dateTo) {

    return ResponseEntity.ok(ratioService.getPackingRatio(logisticCenterId, dateFrom, dateTo));
  }
}
