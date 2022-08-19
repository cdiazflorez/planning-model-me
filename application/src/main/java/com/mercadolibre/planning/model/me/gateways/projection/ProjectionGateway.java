package com.mercadolibre.planning.model.me.gateways.projection;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SaveSimulationsRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Simulation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.BacklogProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogAreaDistribution;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogQuantity;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogQuantityAtSla;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.ProjectedBacklogForAnAreaAndOperatingHour;
import com.mercadolibre.planning.model.me.gateways.projection.deferral.DeferralProjectionStatus;
import java.time.Instant;
import java.util.List;

public interface ProjectionGateway {

  List<ProjectedBacklogForAnAreaAndOperatingHour> projectBacklogInAreas(Instant dateFrom,
                                                                        Instant dateTo,
                                                                        Workflow workflow,
                                                                        List<ProcessName> processes,
                                                                        List<BacklogQuantityAtSla> backlog,
                                                                        List<PlanningDistributionResponse> plannedUnits,
                                                                        List<MagnitudePhoto> throughput,
                                                                        List<BacklogAreaDistribution> backlogDistribution);

  void deferralSaveSimulation(SaveSimulationsRequest request);

  // TODO replace this method signature with types that belong to the domain
  List<BacklogProjectionResponse> getBacklogProjection(BacklogProjectionRequest request);

  List<DeferralProjectionStatus> getDeferralProjectionStatus(Instant dateFrom,
                                                               Instant dateTo,
                                                               Workflow workflow,
                                                               List<ProcessName> processes,
                                                               List<BacklogQuantity> backlogs,
                                                               String wareHouseId,
                                                               String timeZone,
                                                               boolean applyDeviation,
                                                               List<Simulation> simulations);

}
