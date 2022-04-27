package com.mercadolibre.planning.model.me.usecases.sharedistribution;

import com.mercadolibre.planning.model.me.entities.sharedistribution.ShareDistribution;
import com.mercadolibre.planning.model.me.gateways.entity.EntityGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SaveUnitsResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.utils.DateUtils;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Named;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Check historical backlogs and get projections to be saved. */
@Named
@AllArgsConstructor
@Slf4j
public class SaveShareDistribution {

  private final EntityGateway entityGateway;

  private GetMetrics getMetrics;


  public List<SaveUnitsResponse> execute(List<String> warehouseIds, Instant viewDate, int days, Instant now) {

    final Instant dateFrom = viewDate == null ? now.truncatedTo(ChronoUnit.DAYS).plus(24, ChronoUnit.HOURS): viewDate;
    final Instant dateTo = dateFrom.plus(days * 24L, ChronoUnit.HOURS);

    return warehouseIds.stream().map(id -> {
      List<ShareDistribution> list = getMetrics.execute(id, dateFrom, dateTo);
      SaveUnitsResponse saveUnitsResponse;
      if (!list.isEmpty()) {
        try {
          saveUnitsResponse = entityGateway.saveShareDistribution(list, Workflow.FBM_WMS_OUTBOUND);
        } catch (Exception e) {
          saveUnitsResponse = SaveUnitsResponse.builder().response("Error saving").quantitySave(0).build();
          log.error(e.getMessage(), e);
        }
      } else {
        saveUnitsResponse = SaveUnitsResponse.builder().response("Empty records").quantitySave(0).build();
      }
      saveUnitsResponse.setWarehouseId(id);
      return saveUnitsResponse;
    }).collect(Collectors.toList());

  }
}
