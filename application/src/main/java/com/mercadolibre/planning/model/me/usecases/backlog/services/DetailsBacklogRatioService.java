package com.mercadolibre.planning.model.me.usecases.backlog.services;

import static com.mercadolibre.planning.model.me.enums.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static java.util.Collections.emptyMap;
import static java.util.List.of;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogPhotoApiGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PackingRatio;
import com.mercadolibre.planning.model.me.gateways.projection.ProjectionGateway;
import com.mercadolibre.planning.model.me.services.backlog.RatioService;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogMonitorDetails;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Named;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Named
public class DetailsBacklogRatioService extends DetailsBacklogService implements GetBacklogMonitorDetails.BacklogProvider {

  public static final List<ProcessName> PROCESSES = of(PACKING, BATCH_SORTER);

  private final RatioService ratioService;

  public DetailsBacklogRatioService(final BacklogPhotoApiGateway backlogPhotoApiGateway,
                                    final ProjectionGateway projectionGateway,
                                    final RatioService ratioService) {
    super(projectionGateway, backlogPhotoApiGateway);
    this.ratioService = ratioService;
  }

  @Override
  public boolean canProvide(final ProcessName processName) {
    return PROCESSES.contains(processName);
  }

  @Override
  protected Map<Instant, Double> getPackingWallRatios(final BacklogProviderInput input) {
    final Map<Instant, PackingRatio> packingRatios = input.isHasWall()
        ? ratioService.getPackingRatio(input.getWarehouseId(), input.getDateFrom(), input.getDateTo())
        : emptyMap();

    return packingRatios.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, item -> item.getValue().getPackingWallRatio()));
  }
}
