package com.mercadolibre.planning.model.me.usecases.sharedistribution;

import com.mercadolibre.planning.model.me.entities.sharedistribution.ShareDistribution;
import com.mercadolibre.planning.model.me.gateways.entity.EntityGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SaveUnitsResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;
import java.time.ZonedDateTime;
import java.util.List;

@Named
@AllArgsConstructor
@Slf4j
public class SaveShareDistribution {

  private final EntityGateway entityGateway;

  GetMetrics getMetrics;


  public List<SaveUnitsResponse> execute(List<String> warehouseIds, ZonedDateTime dateFrom, ZonedDateTime dateTo) {

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
