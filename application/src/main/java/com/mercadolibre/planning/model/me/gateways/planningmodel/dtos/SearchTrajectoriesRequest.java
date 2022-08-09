package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@EqualsAndHashCode
public class SearchTrajectoriesRequest {

    /**
     * Only trajectories of variables corresponding to this `workflow` will be included in the
     * response.
     */
    private Workflow workflow;

    /**
     * Only trajectories of variables whose {@link MagnitudeType}s is listed here will be included
     * in the response.
     * TODO rename to `magnitudeType`
     */
    private List<MagnitudeType> entityTypes;

    /**
     * Only trajectories of variables corresponding to this `warehouse` will be included in the
     * response.
     */
    private String warehouseId;

    /** Trajectory points before this date will be cropped from the response. */
    private ZonedDateTime dateFrom;

    /** Trajectory points after this date will be cropped from the response. */
    private ZonedDateTime dateTo;

    private Source source;

    /**
     * Only trajectories of variables corresponding to the processes listed here will be included
     * in the response.
     */
    private List<ProcessName> processName;

    /** TODO document this. */
    private List<Simulation> simulations;

    /**
     * Only trajectories of variables whose properties have the specified values will be included
     * in the response.
     * TODO rename to `variableFilters`
     */
    private Map<MagnitudeType, Map<String, List<String>>> entityFilters;
}
