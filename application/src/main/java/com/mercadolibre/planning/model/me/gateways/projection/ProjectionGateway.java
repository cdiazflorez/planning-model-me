package com.mercadolibre.planning.model.me.gateways.projection;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogAreaDistribution;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogQuantityAtSla;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.ProjectedBacklogForAnAreaAndOperatingHour;
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


}
