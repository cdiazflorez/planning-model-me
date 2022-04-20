package com.mercadolibre.planning.model.me.controller.tools;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SaveUnitsResponse;
import com.mercadolibre.planning.model.me.usecases.sharedistribution.SaveShareDistribution;
import com.newrelic.api.agent.Trace;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.util.List;


@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/middleend/share_distribution")
public class ShareDistributionController {


    SaveShareDistribution saveShareDistribution;

    @Trace
    @PostMapping("/execute")
    public ResponseEntity<List<SaveUnitsResponse>> run(@RequestParam @NotNull final List<String> warehouseIds,
                                                       @RequestParam @NotNull final int days) {

        return ResponseEntity.ok(saveShareDistribution.execute(warehouseIds, days));
    }

}
